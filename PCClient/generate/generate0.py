str = '''{"msgID":"signalPhaseAndTimingMessage","intersections":[{"id":"'''

str += '007B' # id
str += '''","status":"00","states":[{"laneSet":"'''

str += '02' # laneset
str += '''","currState":'''

str += '3' # currstate
str += ''',"timeToChange":'''

str += '8' # timeToChange
str += ''',"stateConfidence":"timeLikeklyToChange","yellStateConfidence":"timeLikeklyToChange"},{"laneSet":"02","currState":4,"timeToChange":7,"stateConfidence":"timeLikeklyToChange","yellStateConfidence":"timeLikeklyToChange"},{"laneSet":"FF","currState":127,"timeToChange":127,"stateConfidence":"timeLikeklyToChange","yellStateConfidence":"timeLikeklyToChange"},{"laneSet":"FF","currState":127,"timeToChange":127,"stateConfidence":"timeLikeklyToChange","yellStateConfidence":"timeLikeklyToChange"}]}]}'''

print (str)
