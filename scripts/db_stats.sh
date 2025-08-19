#!/bin/bash

# Default number of documents to show
DEFAULT_DOCS=5
DOCS_TO_SHOW=${1:-$DEFAULT_DOCS}

# Validate input
if ! [[ "$DOCS_TO_SHOW" =~ ^[0-9]+$ ]]; then
    echo "❌ Invalid argument. Usage: $0 [number_of_documents]"
    echo "   Example: $0 10  (shows 10 documents)"
    echo "   Default: $0     (shows 5 documents)"
    exit 1
fi

echo "=== ChromaDB Collection Inspector (SpringAI) ==="
echo ""

# Check if ChromaDB is running
if ! curl -s http://localhost:8000 > /dev/null 2>&1; then
    echo "❌ ChromaDB is not running on http://localhost:8000"
    echo "   Run: docker-compose up -d"
    exit 1
fi

echo "✓ Connected to SpringAiTenant/SpringAiDatabase"
echo ""

# Use Python to inspect ChromaDB
/Users/harit/.pyenv/shims/python3 << EOF
import chromadb
import sys

def format_embedding(embedding):
    """Format embedding vector showing first 15 and last 15 dimensions"""
    try:
        if embedding is None or (hasattr(embedding, '__len__') and len(embedding) == 0):
            return "No embedding"

        # Convert to list if it's a numpy array
        if hasattr(embedding, 'tolist'):
            embedding = embedding.tolist()

        if len(embedding) <= 30:
            # If embedding is short, show all
            return f"[{', '.join(f'{x:.4f}' for x in embedding)}] ({len(embedding)} dimensions)"
        else:
            # Show first 15 ... last 15
            first_15 = ', '.join(f'{x:.4f}' for x in embedding[:15])
            last_15 = ', '.join(f'{x:.4f}' for x in embedding[-15:])
            return f"[{first_15}, ..., {last_15}] ({len(embedding)} dimensions)"
    except Exception as e:
        return f"Embedding format error: {e}"

def format_text(text, max_length=100):
    """Format document text with truncation"""
    if not text:
        return "No text"

    # Replace newlines with spaces for cleaner display
    text = text.replace('\n', ' ').replace('\r', ' ')
    if len(text) <= max_length:
        return text
    return text[:max_length] + "..."

def format_metadata(metadata):
    """Format metadata as key=value pairs"""
    if not metadata:
        return "No metadata"

    items = []
    for key, value in metadata.items():
        if key == 'distance':  # Skip distance as it's not user metadata
            continue
        if isinstance(value, str):
            items.append(f"{key}={value}")
        elif isinstance(value, bool):
            items.append(f"{key}={str(value).lower()}")
        else:
            items.append(f"{key}={value}")

    return ", ".join(items) if items else "No metadata"

try:
    # Connect to SpringAI tenant and database
    client = chromadb.HttpClient(
        host="localhost",
        port=8000,
        tenant="SpringAiTenant",
        database="SpringAiDatabase"
    )

    # List collections
    collections = client.list_collections()

    if not collections:
        print("📭 No collections found in SpringAI tenant/database")
        sys.exit(0)

    # Show collections table
    print("📊 Collections Summary:")
    print("┌─────────────────────────────┬────────────┐")
    print("│ Collection Name             │ Documents  │")
    print("├─────────────────────────────┼────────────┤")

    for collection in collections:
        col = client.get_collection(collection.name)
        count = col.count()
        print(f"│ {collection.name:<27} │ {count:>10} │")

    print("└─────────────────────────────┴────────────┘")
    print("")

    # Show detailed documents for each collection
    for collection in collections:
        col = client.get_collection(collection.name)
        count = col.count()

        if count == 0:
            print(f"📋 Collection '{collection.name}' is empty")
            continue

        docs_to_fetch = min(${DOCS_TO_SHOW}, count)
        print(f"📋 First {docs_to_fetch} Documents from '{collection.name}':")

        # Get documents with all data
        try:
            results = col.get(
                limit=docs_to_fetch,
                include=['documents', 'metadatas', 'embeddings']
            )
        except Exception as e:
            # Fallback without embeddings if there's an issue
            print(f"   Warning: Could not fetch embeddings: {e}")
            results = col.get(
                limit=docs_to_fetch,
                include=['documents', 'metadatas']
            )

        if not results or not results['documents']:
            print("   No documents retrieved")
            continue

        for i in range(len(results['documents'])):
            doc_id = results['ids'][i] if 'ids' in results and len(results['ids']) > i else f"doc_{i}"
            document = results['documents'][i] if len(results['documents']) > i else ""
            metadata = results['metadatas'][i] if 'metadatas' in results and len(results['metadatas']) > i else {}
            embedding = results['embeddings'][i] if 'embeddings' in results and len(results['embeddings']) > i else None

            print("─" * 80)
            print(f"Document {i+1}:")
            print(f"  ID: {doc_id}")
            print(f"  Text: {format_text(document, 120)}")
            print(f"  Embedding: {format_embedding(embedding)}")
            print(f"  Metadata: {format_metadata(metadata)}")

        print("─" * 80)
        print("")

except ImportError:
    print("❌ ChromaDB Python client not installed")
    print("   Install with: pip install chromadb")
    sys.exit(1)

except Exception as e:
    print(f"❌ Error connecting to SpringAI tenant: {e}")
    print("")
    print("Possible causes:")
    print("  • ChromaDB container not running")
    print("  • SpringAI tenant/database doesn't exist")
    print("  • Connection parameters incorrect")
    sys.exit(1)
EOF

echo "💡 Usage: $0 [N]  (where N = number of documents to show)"
echo "   Example: $0 10   (show 10 documents)"
echo "   Default: $0      (show 5 documents)"