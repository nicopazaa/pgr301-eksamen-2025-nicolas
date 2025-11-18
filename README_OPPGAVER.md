# Oppgavetekst PGR301- 2025 Høst

## Den tekniske utfordringen

Dagens løsning basert på AWS Comprehend har flere kritiske svakheter:

**Manglende selskapsspesifikk analyse**: Når en artikkel omtaler flere selskaper, kan tjenesten ikke skille mellom ulike sentiment for hvert selskap. En artikkel med overskriften *"NVIDIA knuser forventningene mens IBM sliter med nedgang"* får en nøytral eller blandet score, i stedet for positivt sentiment for NVIDIA og negativt for IBM.

Med kunstig intelligens kan vi bygge en løsning som forstår kontekst, nyansert språk, og som kan trekke ut selskapsspesifikk sentiment fra komplekse tekster.

## Litt om AWS Bedrock og Nova

**AWS Bedrock** er en tjeneste fra Amazon som gir deg tilgang til ulike **generative KI-modeller** gjennom et felles API. Bedrock fungerer som et **abstraksjonslag** over flere modell-leverandører, slik at du kan bruke språkmodeller fra f.eks. Anthropic, Amazon, Meta og Mistral uten å måtte håndtere infrastrukturen, treningen eller modellforvaltningen selv.

I denne oppgaven skal du bruke **AWS Nova Micro**, en av Amazons nyeste generative AI-modeller tilgjengelig via Bedrock, til å utføre **sentimentanalyse** på tekst. Nova kan forstå naturlig språk og trekke ut mening, tone og kontekst fra tekstlige beskrivelser.

Du skal sende inn et tekstutdrag til Nova gjennom Bedrock, og motta en strukturert respons tilbake. Responsen skal blant annet kunne:

* Identifisere **hvilke selskaper** som omtales i teksten
* Gi en **egen sentimentvurdering** (positiv, negativ, nøytral) for hvert selskap
* Inkludere en **kort begrunnelse** for vurderingen

**Eksempel på input:**
```
"Tesla aksjen stiger kraftig etter sterke kvartalstall, mens Ford sliter med produksjonsproblemer og synkende salg i Europa."
```

**Eksempel på output:**
```json
[
  {
    "company": "TESLA",
    "sentiment": "POSITIVE",
    "confidence": 0.92,
    "reasoning": "Strong quarterly results driving stock price up"
  },
  {
    "company": "FORD",
    "sentiment": "NEGATIVE",
    "confidence": 0.88,
    "reasoning": "Production problems and declining sales in Europe"
  }
]
```

Målet med oppgaven er å bli kjent med hvordan man kan integrere **generative KI-modeller** i en løsning ved hjelp av AWS Bedrock — satt i sammenheng med DevOps prinsipper og teknikker naturligvis.


## Oppgave 1 - Terraform, S3 og Infrastruktur som Kode (15 poeng)

### ️Viktig!

Denne oppgaven oppretter S3-bucketen du trenger for resten av eksamen.

**Hvis du ikke får til denne oppgaven**, kan du opprett bucketen manuelt:

1. Gå til AWS Console → S3
2. Opprett bucket: `kandidat-<ditt-kandidatnr>-data` (eksempel: `kandidat-123-data`)
3. Region: `eu-west-1` (Irland)
4. La alle andre innstillinger være default

Du kan fortsette med oppgave 2-4 selv om oppgave 1 ikke er fullført.

### Kontekst

AiAlpha lagrer tusenvis av analyseresultater i S3. Noen av disse filene er **midlertidige mellomresultater**, mens andre skal **bevares permanent** for videre analyse.

For å unngå høye lagringskostnader og holde dataområdet ryddig, ønsker selskapet å ha én felles S3-bucket med et **egen mappe for midlertidige filer**.

Filer i dette området skal automatisk slettes etter en tid, mens øvrige filer i bucketen skal ligge urørt.

Du skal derfor definere en **lifecycle-strategi** som håndterer denne forskjellen, og etablere løsningen som kode ved hjelp av Terraform.

### Oppgave

Du skal **skrive Terraform-kode** som oppretter og konfigurerer en S3-bucket for analyseresultater, inkludert en **lifecycle-policy** som automatisk håndterer midlertidige filer.

Dokumentasjon på denne ressursen finner dere her;
https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/s3_bucket_lifecycle_configuration

I tillegg skal du sette opp en **GitHub Actions-pipeline** som validerer og kjører infrastrukturen på en trygg og repeterbar måte.

### Rammer og krav

#### Mappestruktur

* Opprett en mappe i repoet ditt: `infra-s3/`
* All Terraform-kode og dokumentasjon skal ligge i denne mappen
* GitHub Actions workflow skal ligge i `.github/workflows/` du kan velge navn på yml filen

#### Infrastruktur (Terraform)

Du skal:

1. Skrive **Terraform-kode** som oppretter en S3-bucket for analyseresultater.

2. Implementere en **lifecycle-strategi** som gjelder for filer under mappen/prefix `midlertidig/` i bucketen. Du kan for eksempel lage regler som:
   * Filer under `midlertidig/` kan også for eksempel automatisk flyttes til en billigere lagringsklasse (f.eks. Glacier) etter et visst antall dager.
    * Filer under `midlertidig/` skal oså slettes automatisk etter en gitt tid.

Filer **utenfor `midlertidig/`** skal ikke berøres av lifecycle-reglene, og blir liggende permanent.

3. Unngå hardkoding av verdier og bruke **variabler** for konfigurasjon (bucket-navn, tidsgrenser osv.).

4. Definere **outputs** som viser relevante verdier (f.eks. bucket-navn, region eller policy-id).

5. Terraform-konfigurasjon skal sørge for at terraform >= 1.5 brukes. Terraform state skal lagres in en S3 backend i bucket `pgr301-terraform-state`


#### CI/CD (GitHub Actions)

Du skal:

1. Lage en workflow i `.github/workflows/` som:
    * Kjører `terraform fmt -check`, `terraform validate` og `terraform plan` på pull-requests
    * Kjører `terraform apply` ved push til main branch

2. Sikre at workflowen kun kjøres ved endringer i `infra-s3/`-mappen.
3. Det kan være lurt å sette opp terraform til også å automatisk kjøre på endringer i `.github/**` katalogen, slik at andringer i workflow fil starter et nytt bygg
4. Workflow må ha access-keys til ditt AWS miljø, dette skal konfigureres ved hjelp av repository secrets.

### Leveranser

* `infra-s3/`-mappe med Terraform-kode, variabler, outputs og README
* `.github/workflows/terraform-s3.yml` (eller lignende navn) for Terraform-pipelinen

> **NB:** Bucketen må eksistere for å gjøre oppgave 2-4. Hvis Terraform feiler, opprett bucketen manuelt i AWS Console  
> (se instruksjoner i toppen av oppgaven).

### Vurderingskriterier

* **Korrekthet:** Terraform-koden fungerer, validerer og kan plan/apply uten feil.
* **Relevans:** Lifecycle-reglene reflekterer formålet med midlertidige og permanente data.
* **Automatisering:** Pipeline-en er strukturert, forutsigbar og trygg å kjøre.
* **Lesbarhet:** Koden og dokumentasjonen er ryddige og forståelige.

## Oppgave 2 - AWS Lambda, SAM og GitHub Actions (25 poeng)

### Kontekst

Den eksisterende SAM-applikasjonen i `sam-comprehend/` bruker Amazon Comprehend for sentimentanalyse. Dette er AiAlphas nåværende løsning som fungerer, men som har begrensninger vi skal utforske.

### Del A (10p): Deploy og test SAM-applikasjonen

**Oppgave:**
1. Utforsk SAM-applikasjonen i `sam-comprehend/` mappen
2. Endre applikasjonen slik at den lagrer data i S3 bucket du lagde i oppgave 1 ved å endre `S3BucketName` parameter i `template.yaml` til å peke på bucketen du opprettet.
3. Kjør applikasjonen lokalt med `sam local invoke` eller `sam local start-api`
4. Deploy applikasjonen til AWS med `sam deploy --guided`
5. Test deployed endepunkt med curl eller Postman

**Eksempel på test:**
```bash
curl -X POST https://YOUR-API-URL/analyze \
  -H "Content-Type: application/json" 
  -d '{"text": "Apple launches groundbreaking new AI features while Microsoft faces security concerns in their cloud platform."}'
```

**Tips:**
- Resultatene lagres i S3 under `s3://<din bucket>/`
- Lambda-funksjonen lagrer analyseresultater under prefix `midlertidig/` i bucketen, slik at lifecycle-policyen fra Oppgave 1 automatisk sletter dem etter den konfigurerte perioden

#### Leveranser:
- **API Gateway URL:** HTTP endpoint for din Lambda-funksjon
- **S3 objekt:** Sti til minst ett lagret analyseresultat (f.eks. `s3://<din bucket>/midlertidig/comprehend-20250115-120000-abc123.json`)

### Del B (15p): Fiks GitHub Actions Workflow

I repositoryet finner du en GitHub Actions workflow i `.github/workflows/sam-deploy.yml`.

**Problemet:** Workflowen oppfører er ikke implementert i henhold til god DevOps praksis.
Den deployer til AWS på **hver eneste pull request**, I tillegg mangler den viktige steg, og hardkoder verdier burde være mulig å overstyre
Du skal nå endre på denne GitHub actions workflowen;

#### Installere SAM i CodeSpaces

```bash
wget https://github.com/aws/aws-sam-cli/releases/latest/download/aws-sam-cli-linux-x86_64.zip
unzip aws-sam-cli-linux-x86_64.zip -d sam-installation
sudo ./sam-installation/install
```

**SAM CLI kommandoer du skal bruke:**
- `sam validate` - Validerer template syntax
- `sam build` - Bygger applikasjonen
- `sam deploy` - Deployer til AWS

**Krav til løsningen:**

**På Pull Requests (validering):**
- Kjør `sam validate` for å sjekke template
- Kjør `sam build` for å verifisere at koden bygger
- **IKKE** deploy til AWS!

**På push til main (deployment):**
- Kjør `sam validate`
- Kjør `sam build`
- Kjør `sam deploy` med deployment til AWS

**Annet:**
- Konfigurer nødvendige GitHub Secrets for AWS credentials
- Bytt ut hardkodede verdier med ditt eget kandidatnummer

#### Leveranser:
- **Workflow-fil:** Lenke til din fikset `.github/workflows/sam-deploy.yml`
- **Successful deploy:** Lenke til en vellykket workflow-kjøring som deployet til AWS (grønn checkmark)
- **PR validation:** Lenke til en PR hvor workflow kun kjørte validering og build (uten deploy)
- **Instrukskjoner til sensor** En beskrivelse på hva *sensor* må gjøre for å få workflow til å kjøre i sin GitHub konto.

## Oppgave 3 - Container og Docker (25 poeng)

### Kontekst

I de tidligere oppgavene har vi jobbet med AWS Comprehend for sentimentanalyse. Nå skal vi ta steget videre til en AI-basert løsning ved hjelp av **AWS Nova** - Amazons nye familie av generative AI-modeller.

#### Litt om AWS Nova

**AWS Nova** er Amazons nyeste generasjon av Foundation Models (grunnmodeller) tilgjengelig via AWS Bedrock. Nova-modellene er designet for å være kostnadseffektive og raske, samtidig som de leverer høy kvalitet på tekstforståelse og generering.

For tekstanalyse tilbyr Nova flere varianter:
- **Nova Micro** - Rask og kostnadseffektiv for enkle tekstoppgaver
- **Nova Lite** - Balansert ytelse for de fleste bruksområder
- **Nova Pro** - Avansert modell for komplekse analyseoppgaver

I denne oppgaven bruker vi **Nova Micro** som er optimalisert for raske og kostnadseffektive tekstanalyser.

#### Fordeler med AI-basert sentimentanalyse

Sammenlignet med tradisjonelle NLP-tjenester som AWS Comprehend, gir AI-modeller som Nova flere viktige fordeler:

1. **Kontekstforståelse**: AI-modeller kan forstå nyansert språk, ironi og komplekse setningsstrukturer som tradisjonelle regelbaserte systemer sliter med.

2. **Selskapsspesifikk analyse**: Mens Comprehend gir ett samlet sentiment for hele teksten, kan AI-modeller identifisere og analysere sentiment for *hvert enkelt selskap* som omtales - akkurat det AiAlpha trenger.

3. **Begrunnelse og transparens**: AI-modellen kan forklare *hvorfor* den kom frem til en bestemt vurdering, noe som gir tillit og mulighet for kvalitetskontroll.

4. **Fleksibilitet**: Vi kan tilpasse instruksjonene (prompts) til våre spesifikke behov uten å måtte trene nye modeller.

#### Applikasjonen du skal containerisere

I mappen `sentiment-docker/` finner du en Spring Boot-applikasjon som:
- Tilbyr et REST API for sentimentanalyse
- Bruker AWS Bedrock med Nova for AI-basert sentimentanalyse
- Gir **selskapsspesifikk sentiment** for hver enkelt virksomhet som omtales
- Lagrer resultater strukturert i S3
- Er klar for metrics og observability (Oppgave 4)

**Slik kjører du applikasjonen lokalt:**

```bash
# Sett nødvendige miljøvariabler
export AWS_ACCESS_KEY_ID=your-key
export AWS_SECRET_ACCESS_KEY=your-secret
export S3_BUCKET_NAME=<din bucket>
```

#### Bygg og kjør med Maven
```
mvn spring-boot:run
```

**Test API-et:**

```bash
## Test API

```bash
curl -X POST http://localhost:8080/api/analyze   -H "Content-Type: application/json"   -d '{"requestId": "test-123", "text":"US stocks tumbled on Thursday, with all major indexes closing lower as investor anxiety around Big Tech and weakening job data intensified. The Nasdaq Composite fell nearly 2%, the S&P 500 dropped 1.1%, and the Dow Jones Industrial Average lost almost 400 points, or 0.8%. A new report from Challenger, Gray & Christmas showed October was the worst month for layoff announcements since 2003, sparking a flight to bonds. The benchmark 10-year Treasury yield slipped below 4.1%, as investors sought safety amid growing economic uncertainty. Tech stocks led the decline, with Nvidia (NVDA) down 3.6%, and fellow chipmakers AMD and Qualcomm also sliding. Qualcomm’s strong earnings failed to offset concerns about lofty AI valuations, especially after Trump administration official David Sacks stated there would be “no federal bailout” for the artificial intelligence industry. His comments came shortly after speculation about potential government support for AI chip investments. Tesla (TSLA) shares fell 3.5% ahead of a high-stakes shareholder vote on Elon Musk’s $1 trillion pay package, which investors ultimately approved amid fears he might step down as CEO if it failed. Meanwhile, Federal Reserve official Beth Hammack cautioned that it was “not obvious” the central bank should cut rates further, signaling ongoing inflation concerns. Treasury yields retreated as her remarks reinforced the perception that monetary policy may remain tight. In corporate news, Charles Schwab announced a $660 million deal to acquire Forge Global Holdings, a platform for private-company shares, while Trump’s administration reached agreements with Eli Lilly and Novo Nordisk to lower prices of popular weight-loss drugs like Zepbound and Wegovy in exchange for expanded Medicare access. Overall, investors face renewed volatility as AI sector momentum cools, layoffs rise, and monetary uncertainty clouds the outlook"}'|jq

```

Legg merke til hvordan AI-modellen:

- Identifiserer **flere separate selskaper** i samme tekst
- Gir **individuell sentiment** for hvert selskap
- Inkluderer **confidence score** som viser hvor sikker modellen er
- Forklarer **begrunnelsen** for hver vurdering

Nå er det din oppgave er å "containerisere" denne applikasjonen med Docker.

### Del A (10p): Containeriser Spring Boot-applikasjonen

**Oppgave:** Lag en Dockerfile for Spring Boot-applikasjonen.

**Krav:**
- Bruk **multi-stage build** for å minimere image-størrelse
- Build stage: Maven med Java 21
- Runtime stage: Amazon Corretto 21 Alpine
- Eksponer port 8080
- Sett ENTRYPOINT til å kjøre JAR-filen
- Publiser container image til din egen DockerHub konto

### Test lokalt

For å teste løsningen lokalt kan du bygge og kjøre Docker-containeren slik:

```bash
docker build -t sentiment-docker .
docker run -e AWS_ACCESS_KEY_ID=$AWS_ACCESS_KEY_ID \
  -e AWS_SECRET_ACCESS_KEY=$AWS_SECRET_ACCESS_KEY \
  -e S3_BUCKET_NAME=<din bucket> \
  -p 8080:8080 sentiment-docker
```
Applikasjonen krever flere **miljøvariabler (environment variables)** .
Disse brukes blant annet til å:

* **Autentisere mot AWS** ved hjelp av `AWS_ACCESS_KEY_ID` og `AWS_SECRET_ACCESS_KEY`.
* **Peke på riktig lagringsplass (din bucket) i S3** gjennom `S3_BUCKET`.

Dette er et eksempel på **god DevOps-praksis**, der konfigurasjon og hemmeligheter holdes **utenfor kildekoden** og heller sendes inn som miljøvariabler ved oppstart.
På den måten kan samme container brukes i flere miljøer (lokalt, test, produksjon) uten at koden må endres.

Test API:

```bash
curl -X POST http://localhost:8080/api/analyze \
  -H "Content-Type: application/json" \
  -d '{"requestId": "test-123", "text": "NVIDIA soars while Intel struggles with declining sales"}'
```

#### Leveranser:
- **Dockerfile:** Fungerende Dockerfile i mappen `sentiment-docker`

### Del B (15p): GitHub Actions workflow for Docker Hub

Lag en GitHub Actions workflow som bygger og publiserer Docker-imaget til Docker Hub.

**Krav:**
1. **Trigger:** Push til **bare** `main` branch
2. **Path filtering:** Kun kjøre når `sentiment-docker/**` endres
3. **Bygg image:** Med Docker
4. **Tagging-strategi:** Velg og implementer en strategi (f.eks. `latest`, `sha-<commit>`, `v1.0.0`)
5. **Publiser:** Push til Docker Hub
6. **Secrets:** Bruk GitHub Secrets for Docker Hub credentials

**Docker Hub setup:**
- Opprett konto på https://hub.docker.com/ ved behov (gratis)
- Legg til secrets i GitHub:
    - `DOCKER_USERNAME`
    - `DOCKER_PASSWORD` eller `DOCKER_TOKEN` (anbefalt)

#### Leveranser:
- **Workflow-fil:** Lenke til `.github/workflows/docker-build.yml`
- **Successful build:** Lenke til vellykket workflow-kjøring (grønn checkmark)
- **Tagging-strategi:** Forklar kort i README_SVAR.md hvorfor du valgte din tagging-strategi
- **Container image navn:** F.eks. `username/sentiment-docker:latest`
- **Beskrivelse for sensor**: Hva må sensor gjøre i sin Fork av ditt repo, for å få at `.github/workflows/docker-build.yml` skal fungere i hans fork?

## Oppgave 4 - Observabilitet, Metrikksamling og Overvåkningsinfrastruktur (25 poeng)

### Kontekst

Observabilitet er et sentralt prinsipp i moderne systemutvikling og drift. I DevOps-metodikken er kontinuerlig tilbakemelding (feedback loops) avgjørende for å opprettholde systemkvalitet og oppdage problemer proaktivt, ideelt sett før brukerne merker dem.

Spring Boot-applikasjonen (`sentiment-docker/`) er allerede konfigurert med Spring Boot Actuator. Din oppgave er å integrere Micrometer-biblioteket for å eksportere applikasjonsmetrikker til Amazon CloudWatch, samt implementere egendefinerte metrikker som er relevante for denne applikasjonen.

### Del A (15 poeng): Implementasjon av Custom Metrics

#### Utgangspunkt

Micrometer CloudWatch Registry er allerede inkludert som avhengighet i `pom.xml`. I `SentimentMetrics.java` finner du et eksempel på implementering av en `Counter`-metrikk som demonstrerer bruk av `MeterRegistry`.

- **viktig** du må endre MetricsConfig klassen og sette inn ditt egent kandidatnavn i linjen  `"cloudwatch.namespace", "SentimentApp"`

Studer implementasjonen i `sentiment-docker/src/main/java/com/aialpha/sentiment/metrics/SentimentMetrics.java`, som demonstrerer:
- Dependency injection av `MeterRegistry` via konstruktør
- Bruk av builder-pattern for å konstruere metrikker (`Counter.builder()`)
- Hvordan man legger til metadata gjennom tags og beskrivelser
- Registrering og inkrementering av målinger

#### Oppgave

Du skal utvide `SentimentMetrics`-klassen med minimum to ytterligere metrikkinstrumenter. Velg blant følgende Micrometer-typer, hvor hver type dekker ulike målebehov:

**`Timer`**: Måler tidsbruk for operasjoner, for eksempel hvor lang tid det tar å kommunisere med AWS Bedrock API. Et Timer-instrument samler både antall hendelser og tidsmålinger, slik at du kan beregne gjennomsnitt, persentiler og maksimalverdier.

**`Gauge`**: Representerer en verdi som kan både øke og reduseres over tid. I motsetning til Counter, som alltid øker, kan en Gauge variere begge veier. Eksempler er antall selskaper funnet i siste analyse, eller størrelsen på en intern buffer.

**`DistributionSummary`**: Viser statistisk fordeling av numeriske verdier. Egnet for å analysere spredning av data, for eksempel fordelingen av konfidensscorer (confidence scores) mellom 0.0 og 1.0. Instrumentet beregner count, sum, max og konfigurerte persentiler.

**`LongTaskTimer`**: Spesialisert instrument for å måle langvarige operasjoner som pågår akkurat nå. I motsetning til standard Timer, som måler fullførte hendelser, lar LongTaskTimer deg observere operasjoner mens de kjører.

#### Implementasjonsveiledning

Du står fritt til å endre eksisterende Java-kode for å gjøre metrikkinsamlingen enklere eller mer naturlig. Dette inkluderer:

- Å utvide `SentimentMetrics`-klassen med nye metoder for instrumentering
- Å kalle metrikk-metoder fra andre steder i koden (ikke bare fra `SentimentController`)
- Å opprette atomiske variabler (f.eks. `AtomicInteger`, `AtomicDouble`) for Gauge-implementasjoner

#### Vurderingskriterier

Vurderingen legger vekt på **korrekt bruk** av metrikkinstrumenter. Valg av instrument skal være faglig begrunnet og passe til det du måler:

- **Counter**: Teller hendelser som alltid øker (allerede implementert for sentimentanalyser)
- **Timer**: Måler varigheten av operasjoner med klar start og slutt (f.eks. AWS Bedrock API responstid)
- **Gauge**: Representerer verdier som kan variere både opp og ned (f.eks. antall selskaper funnet i siste analyse)
- **DistributionSummary**: Analyserer fordelingen av verdier (f.eks. konfidensscorer)

Implementasjonen skal vise at du forstår når de ulike metrikktypene er relevante i produksjonssystemer.

#### Praktiske tips

- Se på den eksisterende Counter-implementasjonen som mal
- Les TODO-kommentarene i `SentimentMetrics.java` for forslag til metrikker

#### Leveranser

- **Screenshot**: CloudWatch Metrics-konsoll som viser dine implementerte custom metrics
- **Teknisk forklaring**: Beskriv designvalgene dine, og forklar hvorfor du valgte spesifikke instrumenttyper for de ulike målingene

### Del B (10 poeng): Infrastruktur for Visualisering og Alarmering
**NB:** For denne oppgaven skal du **IKKE** lage noen GitHub Actions workflow eller CI/CD-pipeline. Terraform-koden skal kjøres manuelt lokalt.

#### Oppgave

Du skal implementere Infrastructure as Code (IaC) ved hjelp av Terraform for å opprette:
1. Et CloudWatch Dashboard for visualisering av applikasjonsmetrikker
2. Minst én CloudWatch Alarm med e-postvarsling via Amazon SNS

#### Krav til infrastrukturen

**1. Mappestruktur**: Opprett mappen `infra-cloudwatch/` i repositoriet for all Terraform-kode knyttet til observabilitetsinfrastruktur.

**2. Terraform-ressurser**: Implementer følgende komponenter som kode:

- **CloudWatch Dashboard** (`aws_cloudwatch_dashboard`): Skal visualisere minimum to av metrikkene du implementerte i Del A.

- **CloudWatch Alarm** (`aws_cloudwatch_metric_alarm`): Skal defineres på én metrikk med en fornuftig terskelverdi (threshold). Eksempler på relevante alarmbetingelser:
  - Latens: Gjennomsnittlig responstid > 5 sekunder over måleperioden
  - Tilgjengelighet: Antall analyseoperasjoner = 0 (indikerer systemfeil)
  - Kvalitet: Konfidensscorer konsekvent under akseptabelt nivå

- **SNS Topic og Subscription** (`aws_sns_topic`, `aws_sns_topic_subscription`): Opprett en notifikasjonskanal med e-post som leveringsmåte.

**3. Dashboard Design**: Widget-valg skal passe til metrikkens type. Tidsseriedata (latens, frekvens) passer godt for LineGraphWidget, mens øyeblikksbilder av verdier (gauge-metrics) kan for  
eksempel vises med NumberWidget.

**4. Alarm Terskelverdi**: Terskelverdier skal defineres basert på systemets normale oppførsel og krav til ytelse. Dokumenter begrunnelsen for de verdiene du velger.

**5. Deployment**: Deploy infrastrukturen manuelt med terraform.

**6. Validering**: Test at alarmen fungerer ved å finne en måte å trigge alarmen på.

#### Tekniske merknader

- Det kan ta noen minutter før metrikker vises i CloudWatch
- SNS e-post-subscription må bekreftes via lenke i e-posten du mottar

#### Leveranser

- **Terraform-kode**: Komplett implementasjon i `infra-cloudwatch/` mappen
- **Dashboard Screenshot**: CloudWatch Console som viser dashboard med faktiske metrikkverdier fra kjørende applikasjon
- **Alarm Screenshot**: CloudWatch Console som viser at alarmen har blitt trigget (ALARM state)
- **E-post Screenshot**: E-post mottatt via SNS som viser at varslingssystemet fungerer fra ende til ende

## Oppgave 5 - KI-assistert Systemutvikling og DevOps-prinsipper (10 poeng)

### Kontekst

I denne eksamenen har du jobbet med AI-teknologi gjennom AWS Bedrock og Nova for sentimentanalyse. Men kunstig intelligens brukes ikke bare i produktene vi bygger - i økende grad brukes KI som verktøy i selve utviklingsprosessen.

AI-assistenter som GitHub Copilot, ChatGPT, Claude og andre verktøy blir stadig mer avanserte og utbredte blant utviklere. Disse verktøyene kan generere kode, foreslå løsninger, skrive tester, lage dokumentasjon, og til og med refaktorere eksisterende systemer. Mange hevder at dette vil revolusjonere programvareutvikling ved å øke produktiviteten dramatisk.

Samtidig introduserer KI-assistert utvikling nye typer risiko: generert kode kan inneholde sikkerhetssårbarheter, ineffektive algoritmer, eller subtile feil som er vanskelige å oppdage. Koden kan mangle dokumentasjon eller bryte med etablerte mønstre i eksisterende systemer. Det er også spørsmål knyttet til ansvar, kvalitet og vedlikeholdbarhet.

### Oppgave

Du skal skrive en drøfting der du vurderer hvordan KI-assistert programvareutvikling påvirker de tre grunnleggende DevOps-prinsippene:

1. **Flyt (Flow)** - Jevn og rask levering av verdi fra idé til produksjon
2. **Feedback** - Rask tilbakemelding på kvalitet, ytelse og brukeropplevelse
3. **Kontinuerlig læring og forbedring** - Systematisk læring fra erfaring og deling av kunnskap

### Problemstillinger du skal adressere

Din drøfting skal analysere både **muligheter** og **utfordringer** ved KI-assistert utvikling, sett i lys av hvert av de tre DevOps-prinsippene.

#### 1. Flyt (Flow)

**Spørsmål å vurdere:**
- Hvordan kan KI-verktøy påvirke hastigheten i utviklingsprosessen? Både positivt og negativt?
- Hvilke flaskehalser i utviklingsflyten kan KI potensielt redusere eller eliminere?
- Kan bruk av KI introdusere *nye* flaskehalser eller problemer i utviklingsflyten?
- Hvordan påvirker KI-generert kode code review-prosessen og deployment-syklusen?

#### 2. Feedback

**Spørsmål å vurdere:**
- Hvordan kan (eller bør) feedback-loops tilpasses når deler av koden er AI-generert?
- Hvilken rolle spiller automatisert testing, overvåkning og metrikker når man bruker KI-assistenter?
- Hvordan kan man oppdage og få tilbakemelding på problemer i AI-generert kode tidlig nok?
- Påvirker KI-verktøy evnen til å lære av feil og forbedre kvaliteten over tid?

#### 3. Kontinuerlig læring og forbedring

**Spørsmål å vurdere:**
- Hvordan påvirker KI-assistanse læringsprosessen for utviklere?
- Kan overdreven bruk av KI-verktøy føre til at utviklere mister dybdekompetanse?
- Hvordan kan organisasjoner sikre kunnskapsdeling og læring når mye kode genereres av AI?
- Hvilke nye ferdigheter må utviklere og team tilegne seg for å bruke KI-verktøy effektivt og trygt?

### Krav til besvarelsen

**Struktur:**
- Innledning som etablerer din overordnede vurdering
- Tre hoveddeler som drøfter hvert DevOps-prinsipp
- Konklusjon med dine subjektive refleksjoner

**Innhold:**
- Balansert drøfting med både fordeler og ulemper
- Konkrete eksempler som illustrerer poengene dine
- Kobling til DevOps-praksis du har lært i kurset
- Gjerne referanse til egne erfaringer fra eksamensoppgavene eller andre prosjekter

**Akademisk kvalitet:**
- Nyanser i argumentasjonen - unngå absolutte påstander uten begrunnelse
- Kritisk refleksjon over både positive og negative aspekter
- Teknisk presisjon i beskrivelser

**Lengde:** 500-800 ord

### Tips til drøftingen
- Du trenger ikke å dekke alle underspørsmålene - de er ment som inspirasjon
- Velg konkrete eksempler som underbygger argumentene dine
- Vis at du forstår sammenhengen mellom DevOps-prinsipper og praktisk utvikling
- Det er rom for ulike meninger - det viktigste er at du begrunner standpunktet ditt godt

### Vurderingskriterier

Besvarelsen vurderes etter:
- **Forståelse av DevOps-prinsipper** (30%): Viser du god forståelse av flyt, feedback og kontinuerlig læring?
- **Kritisk analyse** (30%): Drøfter du både fordeler og utfordringer på en balansert måte?
- **Konkrete eksempler** (20%): Bruker du relevante og illustrative eksempler?
- **Struktur og språk** (20%): Er teksten velorganisert, godt skrevet og innenfor ordgrensen?

### Leveranse

Besvarelsen leveres som en del av `README_SVAR.md` i repositoryet ditt.

## LYKKE TIL OG HA DET GØY MED OPPGAVEN!
