# Gossip

This project is based on "A Gossip-Style Failure Detection Service" https://www.cs.cornell.edu/home/rvr/papers/GossipFD.pdf.

In the paper once a member has been declared faulty, it's removed from the membership list when t-cleanup has expired.
T-cleanup is normally 2 X t-fail, that's why in my implementation t-cleanup is not an option in the config.edn, because 
I'm using  2 X t-fail directly.

I've also added the paremeter `fanout`, in the paper in each cycle each member gossiped with one member.

The idea is simple, in each node you should setup the initial nodes and the parameters.  A configuration file must have:
{
 :initial-nodes [
  {:address "127.0.0.1" :port 5555 :local true}
  {:address "127.0.0.1" :port 5556} 
  {:address "127.0.0.1" :port 5557}
 ]
 :t-fail 5000 
 :cycle 1000
 :fanout 2
}

In members/active-nodes you can get the view of this node.

##Usage

Start up a node with: lein run -c config.edn

If you can test it in your machine you can start three nodes with different ports.


    $ lein run -c config-5555.edn
    $ lein run -c config-5556.edn
    $ lein run -c config-5557.edn


![](https://raw.githubusercontent.com/flopezluis/gossip/master/example.gif)

## License

Copyright © 2016 Félix López Luis

Distributed under the MIT License
