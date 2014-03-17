set -e

openssl genrsa -out /tmp/openbus-temp.key 2048
openssl pkcs8 -topk8 -nocrypt -in /tmp/openbus-temp.key -out $1 -outform DER
rm -f /tmp/openbus-temp.key
