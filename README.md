# PeerBase

For a **tutorial** see: http://cs.berry.edu/~nhamid/p2p

Also: <br>
Nadeem Abdul Hamid. 2007. **A lightweight framework for peer-to-peer programming.** *J. Comput. Sci. Coll.* 22, 5 (May 2007), 98–104.
https://dl.acm.org/doi/10.5555/1229688.1229706


## Java

To run this Java application, from a working directory with the `src/peerbase` folder in the Java classpath, type

```
java -cp ... peerbase.sample.FileShareApp <my-port> <peer-port>
```

The peer node will be launched listening on `<my-port>` and will attempt to connect/register with a peer at `<peer-port>`. (Obviously the first peer created will not have a valid `<peer-port>` to connect to - just put anything.)

Files will actually be transferred from one peer's working directory to another if they exist as named.


## Python

(Written for Python 2.x.)

To run the demo Python application:
```
python filergui.py 9001 10 localhost:9000
```

In general,

```
filergui.py <server-port> <max-peers> <peer-ip>:<port>
```

