"""Stand-in issuer for perf-check/run.sh: writes discovery + JWKS and mints demo tokens.

The key is generated per run and never stored; tokens are valid for three hours.
"""
import json, sys, time, base64, pathlib
import jwt
from cryptography.hazmat.primitives.asymmetric import rsa

out = pathlib.Path(sys.argv[1])
issuer = "http://issuer:8000/realms/securehr"
key = rsa.generate_private_key(public_exponent=65537, key_size=2048)
nums = key.public_key().public_numbers()
b64 = lambda i: base64.urlsafe_b64encode(i.to_bytes((i.bit_length() + 7) // 8, "big")).rstrip(b"=").decode()
realm = out / "issuer/realms/securehr"
(realm / ".well-known/openid-configuration").write_text(json.dumps({
    "issuer": issuer,
    "jwks_uri": issuer + "/protocol/openid-connect/certs",
    "id_token_signing_alg_values_supported": ["RS256"],
    "subject_types_supported": ["public"],
    "response_types_supported": ["code"],
    "authorization_endpoint": issuer + "/protocol/openid-connect/auth",
    "token_endpoint": issuer + "/protocol/openid-connect/token"}))
(realm / "protocol/openid-connect/certs").write_text(json.dumps({"keys": [
    {"kty": "RSA", "kid": "perf", "use": "sig", "alg": "RS256", "n": b64(nums.n), "e": b64(nums.e)}]}))
now = int(time.time())
def token(user, emp_no, roles):
    return jwt.encode({"iss": issuer, "sub": "sub-" + user, "aud": "securehr-api", "iat": now,
                       "exp": now + 3 * 3600, "preferred_username": user, "emp_no": emp_no,
                       "realm_access": {"roles": roles}}, key, algorithm="RS256", headers={"kid": "perf"})
(out / "tokens.json").write_text(json.dumps({
    "employee": token("demo.employee", 10012, ["EMPLOYEE"]),
    "hradmin": token("demo.hradmin", 10005, ["EMPLOYEE", "HR_ADMIN"])}))
