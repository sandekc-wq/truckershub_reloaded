#!/usr/bin/env python3
"""
Certificate Hash Extractor für inetfacts.de
Holt den SHA256-Public-Key-Pin für Certificate Pinning
"""

import ssl
import socket
import base64
import hashlib
from cryptography import x509
from cryptography.hazmat.backends import default_backend

def get_cert_hash(hostname):
    """Holt das Zertifikat von einem Server und gibt den SHA256-Public-Key-Pin zurück"""

    try:
        # Verbinde zum Server und hole Zertifikat
        context = ssl.create_default_context()
        with socket.create_connection((hostname, 443), timeout=5) as sock:
            with context.wrap_socket(sock, server_hostname=hostname) as ssock:
                cert_der = ssock.getpeercert(binary_form=True)

        # Parse das Zertifikat
        cert = x509.load_der_x509_certificate(cert_der, default_backend())

        # Extrahiere Public Key
        public_key_der = cert.public_key().public_bytes(
            encoding=x509.serialization.Encoding.DER,
            format=x509.serialization.PublicFormat.SubjectPublicKeyInfo
        )

        # Berechne SHA256-Hash
        sha256_hash = hashlib.sha256(public_key_der).digest()

        # Encode zu Base64
        b64_hash = base64.b64encode(sha256_hash).decode('utf-8')

        return b64_hash

    except Exception as e:
        print(f"ERROR: {e}")
        return None

if __name__ == "__main__":
    hostname = "inetfacts.de"
    print(f"Hole Certificate-Hash von {hostname}...")

    hash_value = get_cert_hash(hostname)

    if hash_value:
        print(f"\n✅ SHA256-Public-Key-Pin gefunden:\n")
        print(f"sha256/{hash_value}")
        print(f"\nDiesen Hash in SecureHttpClient.kt eintragen:")
        print(f".add(\"inetfacts.de\", \"sha256/{hash_value}\")")
    else:
        print("❌ Konnte Hash nicht ermitteln")
