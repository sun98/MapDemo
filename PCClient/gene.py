def getStr(lat, lng):
    
    blob = 'f'*14 + encodeCor(lat) + encodeCor(lng) + 'f'*12 + encodeNum(0) + encodeNum(0) + 'f'*26

    str = '''{"msgID":"basicSafetyMessage","blob1":"'''
    str += blob
    str += '''","safetyExt":{"pathPrediction":{"radiusOfCurve":32767,"confidence":200}},"status":{"vehicleData":{"height":0,"bumpers":{"frnt":0,"rear":0},"mass":1,"trailerWeight":0,"type":"none"}}}'''
    
    return str


def encodeCor(num):

    res = str(hex(int(num*10000000)))
    res = res[2:]

    if len(res) < 8:
        res = '0'*(8-len(res)) + res

    return res


def encodeNum(num):

    res = str(hex(int(num*10000000)))
    res = res[2:]

    if len(res) < 4:
        res = '0'*(4-len(res)) + res

    return res

#out = getStr(31.027771, 121.421761)
#out = getStr(31.0310306, 121.434667)
#out = getStr(31.0311167, 121.4346)
#out = getStr(31.03115, 121.435033)
#out = getStr(31.0292667, 121.426217)
out = getStr(31.02975, 121.427617)
f = open('broad2.txt', 'w')
f.write(out)
f.close()

