# Chat-Server
<h2>This project inclides:</h2>
<ul>
  <li>Implementations for a thread-pooled NIO chat server, that could maintain multiple connections<br>
      (Tested up to four) with clients.</li>
  <li>Implementatioin for a chat-client<br>(for now only a scripted version that takes a user trough all of the following use cases)
    <ul>
      <li>connecting(i.e. logging in)</li>
      <li>registering</li>
      <li>joining a thread</li>
      <li>sending a message</li>
      <li>disconnecting</li>
    </ul>
  </li>
 </ul>
 
 <h2>Implementation description</h2>
 
 <h3>Implementation of server</h3>
 
 <p>The multithreaded server executes on multiple threads only two of which are not part of a pool. The threads are the following:</p>
 
 <p><strong>Connection acceptance thread:</strong> The server starts by starting the Selection thread, binding the server socket and then listening for new connections.
 Once a new connection has been established between a server and a client, this thread simply passes that socket to the registration queue of the Selection thread.<p>
 
 <p><strong>Selection thread:</strong> Executes a continuous loop of registering and selecting sockets. The registering of the sockets include going through its registration queue and registering each socket for the op of reading. Every time a new socket is put on the queue
the thread's selector is woken up. Once the pending sockets are registered the thread blocks on the <code>selector.select()</code> call.
 If the selector was woken up for registration an if statement guards the handling of the keys. 
 A socket would be selected if it was readable or writable. When a set of keys are selected an iterator goes over them an passing them to the appropriate handler. 
 The handler could be specified by passing a BiFunction to the methods <code>SelectionThread.onReading</code> and <code>SelectionThread.onWriting</code>.
 In the ChatServer class, those are specified to receive a new Runnable from the classes ReaderFactory and WriterFactory. 
 Once a Runnable has been received it's scheduled on fixed thread pool for execution.<br>
 <strong>Important:</strong> before executing the readers/writers  the ops of the selected keys are set to 0 so that it's impossible for a second thread to handle the same key</p>
 
 <p><strong>Reading thread(s):</strong> starts by creating a new message by reading it from the socket channel of the key. 
 After that depending on the <cod>MessageType</code> of the message it's serviced appropriately
  <ul>
    <li>
      <code>Messagetype.CONNECT</code> taking the sender id and password from the message the database is queried for a match. If there is a match,
      the server allocates resources to the client by creating client message queue(every client has its own queue) and sends back the same message that it received as a confirmation. Otherwise, It sends a <code>MessageType.FAILURE</code> that is wasn't identified and terminates the connections.
    </li>
    <li>
      <code>Messagetype.REGISTER</code> the server takes the password from the message and updates the database with a new user. Once the update is made
      the autoincremented id is retrieved and send back to the client so that they know how to identify themselves
    </li>
    <li>
      <code>Messagetype.SEND</code> the server takes the thread id of the message and queries the database for all the people that are part of the same chat thread.
      After that, the message is put into the message queue of all the thread participants who are active. Whenever a message queue receives a message the key to which it's attached has it's ops set to write. This way the selector would pick it up for writing.
      The message is then saved to the database.
    </li>
    <li>
      <code>Messagetype.SEND</code> the name of the thread is taken from the message. First, it's checked if the thread already exists in the database.
      If so, the id of it is sent in the message to the client. Otherwise, a new one is created and its id is sent to the client. The server also saves a hello message to the database so that next time a search for participants in the thread is done, the new participant will be found.
    </li>
    <li>
      <code>Messagetype.DISCONNECT</code> the server retrieves the the sender id from the message and using it removes the message queue and disconnects the socket.
    </li>  
  </ul>
  Any exceptions raised by the reader lead to the disconenction fo the client.</p>
  
   <p><strong>Writing thread(s):</strong> takes in a channel that is ready to be written to and uses the attach to the Selection key queue to write each one of the messages to the socket. the thread synchronizes on each message since one message is reference by multiples message queues and the message's buffer variables a are critical to the sending
   After the messages are sent the ops flag for wringing is removed from the key.</p>


<h3>Client implemtation</h3>

<p>The current implemntation of the client is a shallow one. it's mostly a script of how a usual interaction with the server would occur.
it goes through connecting/registering, joining a new thread(one thread per process), writing messages and receving them and disconnecting.
Just as the server the client has its own thread for receiving incoming messages but thread. The rading thread has a handler for each type of message received.</p>

<p>Some bugs with the client include reading from the commandline empty strings and not receving the new thread comfirmation message</p> 
<h3> Future improvements</h3>
<ul>
  <li>Perfecting the object moddel of the server. Constructing clises with more abstraction and independency from each other</li>
  <li>Privide the ability to chat to multiple threads no just one</li>
<ul>
