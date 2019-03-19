def getStr(lat, lng, speed, heading):
    
    blob = 'f'*14 + encodeCor(lat) + encodeCor(lng) + 'f'*12 + encodeNum(speed) + encodeNum(heading) + 'f'*26

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

# 31.027759  121.422001
print(getStr(31.027759, 121.422001, 0, 0))