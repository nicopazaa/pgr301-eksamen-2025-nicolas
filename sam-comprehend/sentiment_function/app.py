import json
import boto3
import os
from datetime import datetime
import hashlib

# Initialize AWS clients
# Bruker eu-west-1 for Comprehend (der tjenesten normalt er tilgjengelig)
comprehend = boto3.client("comprehend", region_name="eu-west-1")
s3 = boto3.client("s3")

# Bucket-navn kommer fra miljøvariabel satt i template.yaml (S3_BUCKET),
S3_BUCKET = os.environ.get("S3_BUCKET", "kandidat-48-data")


def lambda_handler(event, context):
    """
    Lambda function handler for sentiment analysis using Amazon Comprehend.

    Forventet request-body (API Gateway proxy):
    {
        "text": "Article text to analyze..."
    }

    Funksjonen:
    - Leser tekst fra request
    - Forsøker å kalle Comprehend (DetectSentiment + DetectEntities)
    - Skriver ALLTID et resultat til S3 under prefix "midlertidig/",
      også når Comprehend feiler (f.eks. SubscriptionRequiredException)
    """

    try:
        # Parse request body (API Gateway sender body som string)
        body = event.get("body")
        if isinstance(body, str):
            body = json.loads(body or "{}")
        elif body is None:
            body = {}
        if not isinstance(body, dict):
            body = {}

        text = body.get("text", "")

        if not text:
            return {
                "statusCode": 400,
                "headers": {
                    "Content-Type": "application/json",
                    "Access-Control-Allow-Origin": "*",
                },
                "body": json.dumps({"error": "Text field is required"}),
            }

        # Truncate text if too long (Comprehend har 5000 byte-grense)
        if len(text.encode("utf-8")) > 5000:
            text = text[:5000]

        # Generer et request-id basert på tekstinnhold hvis ikke gitt
        request_id = body.get("requestId") or hashlib.md5(text.encode()).hexdigest()[:8]
        timestamp = datetime.utcnow().strftime("%Y%m%d-%H%M%S")

        result_key = f"midlertidig/comprehend-{timestamp}-{request_id}.json"

        # Basis-resultat som alltid lagres til S3
        analysis_result = {
            "requestId": request_id,
            "timestamp": datetime.utcnow().isoformat(),
            "text_length": len(text),
            "method": "Amazon Comprehend (Statistical)",
        }

        # Prøv å kalle Comprehend – men hvis det feiler (som i din konto),
        # legger vi error inn i resultatet i stedet for å stoppe.
        try:
            sentiment_response = comprehend.detect_sentiment(
                Text=text,
                LanguageCode="en",
            )

            entities_response = comprehend.detect_entities(
                Text=text,
                LanguageCode="en",
            )

            # Extract company names from entities
            companies = []
            for entity in entities_response.get("Entities", []):
                if entity.get("Type") == "ORGANIZATION":
                    companies.append(
                        {
                            "name": entity.get("Text"),
                            "confidence": entity.get("Score"),
                        }
                    )

            analysis_result.update(
                {
                    "overall_sentiment": sentiment_response.get("Sentiment"),
                    "sentiment_scores": sentiment_response.get("SentimentScore", {}),
                    "companies_detected": companies,
                }
            )

        except Exception as ce:
            # Typisk: SubscriptionRequiredException i studentkontoer
            print(f"Comprehend error: {str(ce)}")
            analysis_result["comprehend_error"] = str(ce)

        # Lagre resultat (med eller uten Comprehend-data) i S3
        s3.put_object(
            Bucket=S3_BUCKET,
            Key=result_key,
            Body=json.dumps(analysis_result, indent=2),
            ContentType="application/json",
        )

        # Returner 200 med lokasjon og resultat
        return {
            "statusCode": 200,
            "headers": {
                "Content-Type": "application/json",
                "Access-Control-Allow-Origin": "*",
            },
            "body": json.dumps(
                {
                    "analysis": analysis_result,
                    "s3_location": f"s3://{S3_BUCKET}/{result_key}",
                    "note": "Comprehend is used when available; errors are captured in 'comprehend_error' and results are still stored in S3.",
                }
            ),
        }

    except Exception as e:
        print(f"Unhandled error: {str(e)}")
        return {
            "statusCode": 500,
            "headers": {
                "Content-Type": "application/json",
                "Access-Control-Allow-Origin": "*",
            },
            "body": json.dumps({"error": str(e)}),
        }
