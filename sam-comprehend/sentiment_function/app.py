import json
import boto3
import os
from datetime import datetime
import hashlib

# Initialize AWS clients
comprehend = boto3.client('comprehend', region_name='eu-west-1')
s3 = boto3.client('s3')

S3_BUCKET = os.environ.get('S3_BUCKET', 'pgr301-sentiment-data')

def lambda_handler(event, context):
    """
    Lambda function handler for sentiment analysis using Amazon Comprehend.

    Expects POST request with body:
    {
        "text": "Article text to analyze..."
    }

    Returns sentiment analysis with company detection (basic entity extraction).
    """
    try:
        # Parse request body
        if isinstance(event.get('body'), str):
            body = json.loads(event['body'])
        else:
            body = event.get('body', {})

        text = body.get('text', '')

        if not text:
            return {
                'statusCode': 400,
                'headers': {
                    'Content-Type': 'application/json',
                    'Access-Control-Allow-Origin': '*'
                },
                'body': json.dumps({
                    'error': 'Text field is required'
                })
            }

        # Truncate text if too long (Comprehend has a 5000 byte limit)
        if len(text.encode('utf-8')) > 5000:
            text = text[:5000]

        # Detect sentiment using Amazon Comprehend
        sentiment_response = comprehend.detect_sentiment(
            Text=text,
            LanguageCode='en'
        )

        # Detect entities (including organizations/companies)
        entities_response = comprehend.detect_entities(
            Text=text,
            LanguageCode='en'
        )

        # Extract company names from entities
        companies = []
        for entity in entities_response.get('Entities', []):
            if entity['Type'] == 'ORGANIZATION':
                companies.append({
                    'name': entity['Text'],
                    'confidence': entity['Score']
                })

        # Build analysis result
        analysis_result = {
            'timestamp': datetime.utcnow().isoformat(),
            'overall_sentiment': sentiment_response['Sentiment'],
            'sentiment_scores': sentiment_response['SentimentScore'],
            'companies_detected': companies,
            'method': 'Amazon Comprehend (Statistical)',
            'text_length': len(text)
        }

        # Store result in S3
        result_key = f"midlertidig/comprehend-{datetime.utcnow().strftime('%Y%m%d-%H%M%S')}-{hashlib.md5(text.encode()).hexdigest()[:8]}.json"

        s3.put_object(
            Bucket=S3_BUCKET,
            Key=result_key,
            Body=json.dumps(analysis_result, indent=2),
            ContentType='application/json'
        )

        # Return response
        return {
            'statusCode': 200,
            'headers': {
                'Content-Type': 'application/json',
                'Access-Control-Allow-Origin': '*'
            },
            'body': json.dumps({
                'analysis': analysis_result,
                's3_location': f"s3://{S3_BUCKET}/{result_key}",
                'note': 'This uses statistical methods. Companies are detected but sentiment is not per-company.'
            })
        }

    except Exception as e:
        print(f"Error: {str(e)}")
        return {
            'statusCode': 500,
            'headers': {
                'Content-Type': 'application/json',
                'Access-Control-Allow-Origin': '*'
            },
            'body': json.dumps({
                'error': str(e)
            })
        }
