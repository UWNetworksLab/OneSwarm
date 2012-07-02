package edu.washington.cs.oneswarm.f2f.socks;

class SocksConstants {
    static class Authentication {
        static final byte NO_AUTHENTICATION = 0x00;
        static final byte GSSAPI = 0x01;
        static final byte USERNAME_PASSWORD = 0x02;
        static final byte NO_ACCEPTABLE_METHOD = (byte)0xff;
    }

    static class Version {
        static final byte SOCKS_4 = 0x04;
        static final byte SOCKS_5 = 0x05;
    }

    static class Status {
        static final byte REQUEST_GRANTED = 0x00;
        static final byte GENERAL_FAILURE = 0x01;
        static final byte CONNECTION_NOT_ALLOWED_BY_RULESET = 0x02;
        static final byte NETWORK_UNREACHABLE = 0x03;
        static final byte HOST_UNREACHABLE = 0x04;
        static final byte CONNECTION_REFUSED_BY_DESTINATION_HOST = 0x05;
        static final byte TLL_EXPIRED = 0x06;
        static final byte COMMAND_NOT_SUPPORTED = 0x07;
        static final byte ADDRESS_TYPE_NOT_SUPPORTED = 0x08;
    }
    
    static class Command{
        static final byte ESTABLISH_TCP_STREAM_CONNECTION = 0x01;
        static final byte ESTABLISH_TCP_PORT_BINDING = 0x02;
        static final byte ASSOCIATE_UDP_PORT = 0x03;
    }
    
    static class AddressType{
        static final byte IPv4 = 0x01;
        static final byte DOMAIN_NAME = 0x03;
        static final byte IPv6 = 0x04;
    }
}
