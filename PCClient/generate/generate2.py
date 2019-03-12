def getStr(blob):
    str = '''{"msgID":"basicSafetyMessage","blob1":"'''
    str += blob
    str += '''","safetyExt":{"pathPrediction":{"radiusOfCurve":32767,"confidence":200}},"status":{"vehicleData":{"height":0,"bumpers":{"frnt":0,"rear":0},"mass":1,"trailerWeight":0,"type":"none"}}}'''

    return str


def getBlob(lat, lng, speed, heading):
    blob = '000--'

    return blob


def getFrame(lat, lng, speed, heading):
    
    return getStr(getBlob(lat, lng, speed, heading))


for time in range(1000):
    pass