
from subprocess import Popen, PIPE
from Queue import Queue, Empty
from threading import Thread

output = ""

def processWatcher(stream):
	global output
	for line in stream:
		output += line
	if not stream.closed:
		stream.close()

args = ["ls"]
sub = Popen(args, stdout=PIPE, stderr=PIPE)

Thread(target=processWatcher, name='stdout-watcher', args=(sub.stdout)).start()
Thread(target=processWatcher, name='stderr-watcher', args=(sub.stderr)).start()

def printQueue():
	while True:
		print output

Thread(target=printQueue, name='printer').start()
