all: clean
	mkdir bin
	javac -classpath /home/phao3/protobuf/protobuf-3.4.0/java/core/target/protobuf.jar -d bin/ src/Controller.java src/Sender.java src/Receiver.java src/Branch.java src/Bank.java src/ReceiverHandler.java

clean:
	rm -rf bin/
