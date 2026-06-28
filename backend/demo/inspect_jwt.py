import zipfile, os, re
path = os.path.expanduser('~/.m2/repository/io/jsonwebtoken/jjwt-api/0.12.5/jjwt-api-0.12.5.jar')
with zipfile.ZipFile(path, 'r') as z:
    for entry in sorted(z.namelist()):
        if 'io/jsonwebtoken/Jwts' in entry or 'io/jsonwebtoken/JwtParserBuilder' in entry or 'io/jsonwebtoken/JwtParser' in entry:
            print(entry)
    data = z.read('io/jsonwebtoken/Jwts.class')
    names = sorted({s.decode('ascii', 'ignore') for s in re.findall(rb'[A-Za-z_][A-Za-z0-9_]+', data) if b'parser' in s or b'build' in s or b'parseClaims' in s or b'parse' in s})
    print('--- names ---')
    for name in names:
        print(name)
