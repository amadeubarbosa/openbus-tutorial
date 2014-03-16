set -e

openssl req -new -x509 -key $1 -keyform DER  -out $2 -outform DER
