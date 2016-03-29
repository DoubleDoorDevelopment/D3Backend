#!/usr/bin/python
import backend
if __name__ == "__main__":
	backend.init()
	backend.app.run()
	backend.connectToDB()
	