# PeerBase

See: http://cs.berry.edu/~nhamid/p2p

To run this Java application, from a working directory with the `src/peerbase` folder in the Java classpath, type

```
java -cp ... peerbase.sample.FileShareApp <my-port> <peer-port>
```

The peer node will be launched listening on `<my-port>` and will attempt to connect/register with a peer at `<peer-port>`. (Obviously the first peer created will not have a valid `<peer-port>` to connect to - just put anything.)

Files will actually be transferred from one peer's working directory to another if they exist as named.


