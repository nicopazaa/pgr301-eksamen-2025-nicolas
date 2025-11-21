# README_SVAR

## Oppgave 1 – Terraform, S3 og infrastruktur som kode

### Arkitektur og valg

Målet i oppgaven var å etablere én felles S3-bucket for analyseresultater, der midlertidige filer under `midlertidig/` håndteres automatisk med lifecycle-regler, mens øvrige filer bevares permanent.

Jeg har brukt Terraform i mappen `terraform/infra-s3` til å:

- Opprette en S3-bucket for analyseresultater:
  - Navn: `kandidat-48-data`
  - Region: `eu-north-1`
- Konfigurere Terraform til å bruke:
  - `required_version = ">= 1.5.0"`
  - S3-backend for state i en egen bucket (for eksempel `pgr301-terraform-state-nicopazaa-01`) med egen key for `infra-s3`.

Jeg valgte `eu-north-1` som region for å være konsistent med resten av oppsettet (Lambda, API Gateway etc.), men resten av Terraform-koden ville fungert tilsvarende i `eu-west-1` som oppgaven foreslår.

### Lifecycle-policy for `midlertidig/`

Terraform-koden definerer en `aws_s3_bucket_lifecycle_configuration` der jeg kun matcher objekter under prefix `midlertidig/`:

- Objekter under `midlertidig/`:
  - Transition til en billigere lagringsklasse (f.eks. GLACIER) etter `var.transition_days` (standard 7 dager).
  - Sletting (expiration) etter `var.expiration_days` (standard 30 dager).
- Objekter i rot av bucketen (utenfor `midlertidig/`):
  - Ingen lifecycle-regler, de blir liggende permanent som langsiktig historikk.

Alle verdier som kan variere (bucket-navn, region, antall dager) er lagt som `variable` i `variables.tf`, med fornuftige default-verdier. `outputs.tf` eksponerer blant annet bucket-navn og region slik at de kan brukes som referanse i andre deler av systemet eller verifiseres enkelt etter `terraform apply`.

### CI/CD med GitHub Actions for Terraform

Jeg har opprettet workflow-filen `.github/workflows/terraform-s3.yml` som automatiserer Terraform-kjøringen:

- Triggere:
  - `push` til `main` når filer i `terraform/infra-s3/**` eller selve workflow-filen endres.
  - `pull_request` når samme filer endres.
- På både PR og push:
  - `terraform fmt -check` for å sikre format.
  - `terraform validate` for syntaks og konfigurasjon.
  - `terraform plan` med `-var "bucket_name=kandidat-48-data"`.
- Kun på `push` til `main`:
  - `terraform apply -auto-approve` med samme variabel.

Workflowen bruker `hashicorp/setup-terraform` til å sette versjon (>=1.5) og `aws-actions/configure-aws-credentials` til å konfigurere AWS med secrets:

- `AWS_ACCESS_KEY_ID`
- `AWS_SECRET_ACCESS_KEY`

Dette gir en trygg og repeterbar måte å kjøre infrastrukturendringer på, der PR-er kun genererer plan og main-grenen faktisk apply-er.

---

## Oppgave 2 – AWS Lambda, SAM og GitHub Actions

### Oppgave 2A – Deploy og test av SAM-applikasjonen

SAM-applikasjonen ligger i `sam-comprehend/` og består av:

- `template.yaml` – definerer Lambda-funksjonen og API Gateway-endepunktet.
- `sentiment_function/app.py` – Python-koden som kaller Comprehend (når tilgjengelig) og lagrer resultater i S3.

I `template.yaml` har jeg satt parameteren:

"""```yaml
Parameters:
  S3BucketName:
    Type: String
    Default: kandidat-48-data
    Description: S3 bucket for storing sentiment analysis results (must exist, e.g. created by Terraform or manually in S3) """

Lambda-funksjonen får miljøvariabelen S3_BUCKET satt til denne parameteren, og bruker den til å lagre resultater i s3://kandidat-48-data/midlertidig/...

cd sam-comprehend
sam build
sam deploy --guided

Jeg valgte:

Stack name: aialpha-comprehend-48

Region: eu-north-1

Parameter S3BucketName: kandidat-48-data

ter deploy fikk jeg følgende API Gateway-endepunkt:

API Gateway URL:
https://gz51wg3bj8.execute-api.eu-north-1.amazonaws.com/Prod/analyze

Håndtering av Comprehend-feil

Kontoen jeg brukte til eksamen har ikke aktivert abonnement for Amazon Comprehend. Det gir følgende feil når Lambda prøver å kalle tjenesten:

SubscriptionRequiredException: The AWS Access Key Id needs a subscription for the service

Jeg har ikke fått utdelt en egen AWS-bruker/konto med aktivert Comprehend-abonnement, og kan derfor ikke selv aktivere tjenesten i denne kontoen. For å håndtere dette robust har jeg oppdatert app.py slik at funksjonen:

Leser text fra request-body.

Prøver å kalle comprehend.detect_sentiment og detect_entities (region eu-west-1).

Fanger opp alle Comprehend-feil (inkludert SubscriptionRequiredException), og legger feilen i feltet comprehend_error.

Alltid lagrer et JSON-resultat til S3 under midlertidig/, selv om Comprehend ikke er tilgjengelig.

Eksempel på kall jeg har gjort:
curl -X POST https://gz51wg3bj8.execute-api.eu-north-1.amazonaws.com/Prod/analyze \
  -H "Content-Type: application/json" \
  -d '{"text": "Apple launches groundbreaking new AI features while Microsoft faces security concerns in their cloud platform."}'

Eksempelrespons fra API-et:
{
  "analysis": {
    "requestId": "2718ec4f",
    "timestamp": "2025-11-21T05:12:47.535206",
    "text_length": 110,
    "method": "Amazon Comprehend (Statistical)",
    "comprehend_error": "An error occurred (SubscriptionRequiredException) when calling the DetectSentiment operation: The AWS Access Key Id needs a subscription for the service"
  },
  "s3_location": "s3://kandidat-48-data/midlertidig/comprehend-20251121-051247-2718ec4f.json",
  "note": "Comprehend is used when available; errors are captured in 'comprehend_error' and results are still stored in S3."
}

Det viktige her er at:

API-et fungerer (API Gateway → Lambda).

Resultatet lagres i riktig bucket/prefix:

S3-objekt:
s3://kandidat-48-data/midlertidig/comprehend-20251121-051247-2718ec4f.json

Lifecycle-regelen fra Oppgave 1 vil dermed treffe disse midlertidige filene.

Merk om Amazon Comprehend-tilgangen:

Feilen SubscriptionRequiredException skyldes at jeg ikke har fått utdelt en AWS-bruker med aktivert Comprehend-abonnement i denne eksamenskontoen. Selve infrastrukturen og koden er likevel satt opp i tråd med oppgaven, og i et miljø med aktivert Comprehend ville JSON-filen i S3 inneholdt faktiske sentiment- og entity-data i stedet for bare comprehend_error.

Oppgave 2B – Fiks av GitHub Actions workflow for SAM

Jeg har oppdatert .github/workflows/sam-deploy.yml slik at workflowen følger god DevOps-praksis:

Triggere:

push til main når filer i sam-comprehend/** eller workflow-filen endres.

pull_request når samme filer endres.

På pull request (kun validering):

Installerer SAM CLI.

sam validate for å sjekke template-syntaks.

sam build for å verifisere at koden bygger.

Ingen deploy til AWS.

På push til main (deployment):

Samme steg som PR (validate + build).

Konfigurerer AWS credentials med aws-actions/configure-aws-credentials.

Kjører sam deploy med:

--stack-name aialpha-comprehend-48

--region eu-north-1

--capabilities CAPABILITY_IAM

--parameter-overrides S3BucketName=kandidat-48-data

--resolve-s3 og --no-fail-on-empty-changeset

AWS-legitimasjon leses fra repo-secrets:

AWS_ACCESS_KEY_ID

AWS_SECRET_ACCESS_KEY

Instruksjoner til sensor

For at workflowen skal fungere i sensor sin fork, må han:

Opprette repo-secrets:

AWS_ACCESS_KEY_ID

AWS_SECRET_ACCESS_KEY
som peker på en bruker med rettigheter til SAM-deploy (CloudFormation, Lambda, API Gateway, S3).

Sørge for at det finnes en S3-bucket han vil bruke som analysebucket (eventuelt kandidat-XXX-data) og justere parameteren S3BucketName i workflowen og/eller template.yaml.

Oppgave 3 – Container og Docker
Oppgave 3A – Containerisering av Spring Boot-applikasjonen

I mappen sentiment-docker/ ligger en Spring Boot-applikasjon som bruker AWS Bedrock/Nova for selskapsspesifikk sentimentanalyse og lagrer resultater i S3.

Jeg har laget en multi-stage Dockerfile:

Build stage (Maven + Java 21):

Base image: maven:3.9.x-eclipse-temurin-21 (eller tilsvarende).

Kopierer pom.xml og src/.

Kjører: mvn clean package -DskipTests
Output er en fat JAR i target/.

Runtime stage (slank JRE):

Base image: eclipse-temurin:21-jre (slank runtime for Java 21).

Kopierer JAR fra build-staget til /app/app.jar.

Eksponerer port 8080.

Setter opp entrypoint:
ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar app.jar"]

Applikasjonen leser konfigurasjon fra environment-variabler, f.eks.:

AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY

AWS_REGION

S3_BUCKET_NAME

Dette følger god DevOps-praksis: samme image kan brukes i ulike miljøer uten å endre koden – det er bare environment-variabler som varierer.

Eksempel på lokal test:
docker build -t sentiment-docker ./sentiment-docker

docker run \
  -e AWS_ACCESS_KEY_ID=$AWS_ACCESS_KEY_ID \
  -e AWS_SECRET_ACCESS_KEY=$AWS_SECRET_ACCESS_KEY \
  -e AWS_REGION=eu-north-1 \
  -e S3_BUCKET_NAME=kandidat-48-data \
  -p 8080:8080 \
  sentiment-docker

  Deretter kan API-et testes med:

curl -X POST http://localhost:8080/api/analyze \
  -H "Content-Type: application/json" \
  -d '{"requestId": "test-123", "text": "NVIDIA soars while Intel struggles with declining sales"}'
  

Oppgave 3B – GitHub Actions workflow for Docker Hub

For å automatisere bygg og publisering til Docker Hub har jeg laget .github/workflows/docker-ci.yml.

Trigger:

push til main.

Kun når filer i sentiment-docker/** eller selve workflow-filen endres.

Steg i workflow:

actions/checkout@v4 – sjekker ut repo.

docker/login-action@v3 – logger inn på Docker Hub med secrets:

DOCKERHUB_USERNAME

DOCKERHUB_PASSWORD

docker/setup-buildx-action@v3 – setter opp Buildx.

docker/build-push-action@v5 – bygger og pusher image fra sentiment-docker/.

Tagging-strategi:
Workflowen tagger bildet som:

nicopazaa/aialpha-sentiment:latest

nicopazaa/aialpha-sentiment:${{ github.sha }}

Begrunnelse:

latest er praktisk for manuelle tester: docker pull nicopazaa/aialpha-sentiment:latest.

SHA-taggen er en immutabel referanse til nøyaktig commit. Det er nyttig i DevOps for reproduksjon, debugging og eventuelle rollback-scenarier.

Instruksjoner til sensor

For at Docker-workflowen skal fungere i sensor sin fork må han:

Opprette et repository på Docker Hub, f.eks. SENSORUSER/aialpha-sentiment.

Legge inn repo-secrets:

DOCKERHUB_USERNAME – Docker Hub-brukernavn.

DOCKERHUB_PASSWORD – token/passord for push.

Eventuelt endre tags: i workflowen til sitt eget namespace, f.eks.: 
tags: |
  SENSORUSER/aialpha-sentiment:latest
  SENSORUSER/aialpha-sentiment:${{ github.sha }}


Oppgave 4 – Observabilitet, metrikksamling og overvåkningsinfrastruktur
Oppgave 4A – Implementasjon av custom metrics

Spring Boot-applikasjonen i sentiment-docker/ bruker Micrometer til å eksponere metrics. I MetricsConfig har jeg konfigurert en CloudWatch-registry med namespace:

kandidat-48-SentimentApp

Dette er viktig fordi samme namespace brukes i Terraform for dashboard og alarm.

I klassen SentimentMetrics har jeg implementert flere instrumenter:

Counter – sentiment.analysis.total

Teller hvor mange sentimentanalyser som er utført.

Økes for hvert kall til API-et.

Passer fordi en Counter kun skal øke (eventuelt resettes ved restart) og representerer volum.

Timer – sentiment.analysis.duration

Måler hvor lang tid en analyse tar – fra forespørsel til svar.

Gir både antall kall, sum tid og mulighet for å hente gjennomsnitt/percentiler.

Timer er riktig verktøy for å overvåke latens mot Bedrock/Nova eller Comprehend.

Gauge – antall selskaper i siste analyse

Bruker en AtomicInteger som Gauge som oppdateres med antall selskaper funnet i siste request.

Gauge er valgt fordi verdien kan både øke og synke over tid (noen tekster har mange selskaper, andre har ingen).

DistributionSummary – sentiment.analysis.confidence

Registrerer modellens confidence-score (mellom 0.0 og 1.0) for hver enkelt vurdering.

DistributionSummary samler count, sum, max og persentiler, slik at man kan se fordelingen av hvor “trygg” modellen er over tid.

Metrikkene registreres i relevante deler av koden (for eksempel i controller/service-laget rundt kallet til Nova/Comprehend). Resultatet er at vi får et godt bilde av både volum (Counter), ytelse (Timer) og kvalitetsindikatorer (Gauge/DistributionSummary).

Oppgave 4B – Infrastruktur for dashboard og alarm (Terraform)

I terraform/infra-cloudwatch har jeg skrevet Terraform for observabilitetsinfrastruktur:

SNS-topic og e-post-abonnement

aws_sns_topic.alerts – SNS-topic for varsler.

aws_sns_topic_subscription.alerts_email – e-postsubscription basert på var.alert_email.

Jeg har bekreftet subscription gjennom e-post (AWS SNS → “Subscription confirmed”).

CloudWatch dashboard – aws_cloudwatch_dashboard.sentiment

Dashboardet viser minst to av custom metrics i namespace kandidat-48-SentimentApp:

sentiment.analysis.total som time series (Sum per minutt) – gir oversikt over trafikkvolum.

sentiment.analysis.duration som time series (Average) – gir oversikt over gjennomsnittlig responstid.

Dashboardet er definert som dashboard_body med jsonencode, og bruker variabelen var.aws_region for å matche regionen applikasjonen kjører i.

CloudWatch alarm – aws_cloudwatch_metric_alarm.high_latency

Overvåker:

namespace = "kandidat-48-SentimentApp"

metric_name = "sentiment.analysis.duration"

statistic = "Average"

Parametre:

period = 60 sekunder

evaluation_periods = 3

threshold = 2000 (ms)

comparison_operator = "GreaterThanThreshold"

Alarmen er koblet til SNS-topic aws_sns_topic.alerts.arn via alarm_actions.

Begrunnelse for terskelverdier:

For en web-API-baserte analyseløsning bør typiske responstider ligge klart under 2 sekunder.

Ved å sette grensen til 2000 ms og kreve 3 evalueringer på rad, unngår vi falske positiver ved sporadiske spikes, men fanger opp vedvarende ytelsesproblemer (f.eks. treg modell, nettverksproblemer eller throttling).

Terraform-koden kjøres manuelt med:
cd terraform/infra-cloudwatch
terraform init
terraform plan -var "alert_email=<min-epost>"
terraform apply -var "alert_email=<min-epost>"

Etter at applikasjonen har kjørt en stund, dukker metrikker opp i CloudWatch under kandidat-48-SentimentApp, dashboardet viser grafene, og alarmen vil gå i ALARM-state dersom responstidene overstiger terskelen over tid. Via SNS får jeg e-postvarsel når alarmen utløses.

Oppgave 5 – Refleksjon rundt KI-assistert DevOps
Innledning

I denne eksamenen har jeg brukt AI-verktøy (som ChatGPT/Copilot) aktivt gjennom hele løpet – fra å sette opp Terraform-infrastruktur og SAM-applikasjon, til Docker-bygg, GitHub Actions og observability med Micrometer og CloudWatch. Hensikten har ikke vært å la AI “gjøre alt”, men å bruke den som en sparringpartner som kan foreslå mønstre, peke på feil og forklare alternativer. Nedenfor diskuterer jeg hvordan dette har påvirket de tre DevOps-prinsippene flyt, feedback og kontinuerlig læring.

Flyt (Flow)

Målet med flyt er å få endringer raskt og trygt fra idé til produksjon. AI har hjulpet meg å øke tempoet på flere måter:

Mønstre og “boilerplate”: I stedet for å slå opp dokumentasjon hver gang, lot jeg AI foreslå komplette eksempler på Terraform-filer, GitHub Actions-workflows og Dockerfile. Deretter tilpasset jeg navn, regioner og bucket-navn til eksamenskonteksten. Det ga meg raskt en fungerende “happy path” for hele pipeline-kjeden.

Fjerning av flaskehalser: Når jeg satt fast på konkrete feil (for eksempel Terraform-backend mot S3, SAM-deploy med manglende parameter-overrides, og bygg av Docker-image med riktig Java-versjon), kunne AI peke rett på årsaken og foreslå små, presise endringer. Det reduserte tiden jeg ellers ville brukt på å google og prøve meg frem.

Automatisering i stedet for manuelle steg: AI hjalp meg å strukturere GitHub Actions slik at både Terraform, SAM og Docker blir kjørt automatisk på push til main. Det støtter DevOps-tanken om at det alltid er bedre med en repeterbar pipeline enn manuelle konsoll-klikk.

Samtidig finnes det risikoer: kvaliteten på flyten blir bare god hvis jeg forstår hva som skjer. Hvis jeg blindt hadde akseptert alle forslag, kunne jeg endt med hardkodede hemmeligheter, feil regioner eller ressursnavn som ikke stemmer med selve oppgaveteksten. Derfor har jeg hele tiden validert forslagene ved å kjøre terraform plan, sam validate, lokale builds med sam build og ved å sjekke at workflow-runs faktisk ble grønne i GitHub Actions.

Feedback

DevOps handler også om rask og tydelig feedback fra systemene. Her har AI vært nyttig på to nivåer:

Teknisk feedback i pipeline: Når en workflow feilet, brukte jeg AI til å tolke loggene og få forklart hva feilmeldingene egentlig betydde. For eksempel fikk jeg hjelp til å se forskjell på en reell konfigurasjonsfeil og en midlertidig GitHub-feil (HTTP 5xx i checkout-steget). Dermed kunne jeg prioritere riktig – noen feil måtte fikses i koden, andre kunne ignoreres eller bare kjøres på nytt.

Observability-design: I applikasjonskoden foreslo AI konkrete Micrometer-mønstre – Counter, Timer, Gauge og DistributionSummary – og hvordan de kunne brukes til å måle antall analyser, responstid, antall selskaper funnet og modellens confidence. Samtidig hjalp AI meg å definere et CloudWatch-dashboard og en alarm i Terraform som matcher disse metrikkene. Resultatet er en løsning der hvert API-kall gir måledata, og der jeg får alarm hvis responstiden blir for høy.

En utfordring er at AI ikke kjenner eksakt runtime-miljø: det vet ikke hvilke metrikker som faktisk kommer inn i CloudWatch, eller hvordan applikasjonen oppfører seg under last. Derfor er menneskelig verifikasjon viktig: jeg må selv sjekke dashboardet, se at navn på metrikker stemmer og at alarmen er fornuftig satt (for eksempel 2 sekunders terskel over 3 datapunkter).

Kontinuerlig læring og forbedring

Det tredje prinsippet handler om å lære og forbedre seg over tid. Her opplever jeg AI som en sterk læringsakselerator, så lenge jeg bruker det bevisst:

Forklaringer, ikke bare kode: Jeg har bevisst bedt om forklaringer sammen med kode, for eksempel hvorfor Terraform-backend bør ligge i egen S3-bucket, eller hvordan SAM-deploy håndterer --parameter-overrides. Det gjør at jeg forstår mønstrene, ikke bare kopierer dem.

Eksperimentering med trygghetsnett: Jeg kunne teste alternative løsninger – for eksempel ulike måter å strukturere workflows eller Micrometer-metrikkene på – og få AI til å sammenligne fordeler og ulemper. Deretter valgte jeg den varianten som både løser eksamenskravene og er i tråd med “best practice”.

Bevissthet rundt svakheter: Jeg har også erfart at AI iblant kan foreslå kode som ikke passer konteksten (feil region, gamle versjoner, eller eksempler fra andre skyløsninger). Dette tvinger meg til å være kritisk, lese dokumentasjon og teste endringene i mitt eget miljø. På den måten blir AI mer som en “junior som skriver raskt”, mens jeg selv har ansvaret for design og kvalitet.

Avslutning

Samlet sett opplever jeg at KI-assistert utvikling kan støtte alle de tre DevOps-prinsippene i denne oppgaven: raskere flyt fra idé til kjørende løsning, bedre feedback gjennom automatiserte pipelines og observability, samt kontinuerlig læring gjennom forklaringer og eksperimentering. Samtidig er det tydelig at AI ikke kan erstatte forståelse, dømmekraft og ansvar. Rollen min som utvikler/SRE blir kanskje enda viktigere: å formulere gode spørsmål, oversette generelle forslag til eksamensspesifikke krav, og hele tiden verifisere at infrastrukturen er sikker, robust og i tråd med oppgaveteksten.