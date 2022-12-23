package dhcpserver;

import java.net.*;
import java.util.*;
import java.io.*;

public class Server 
{
    private DatagramSocket server;
    private byte[] gateway;
    private byte[] subnetmask;
    private byte[] dns;
    private int renewaltime;
    private int rebindingtime;
    private int leasetime;
    private Vector<byte[]>mac;
    private Vector<byte[]>ip;
    private Vector<Long>starttime;
    private Vector<byte[]>reservedip;
    private Vector<byte[]>reservedmac;
    private Vector<byte[]>declinemac;
    private Vector<byte[]>declineip;
    private Vector<Message>message;
    private long serverstart;
    
    //Constructor
    public Server(byte[] g,byte[] s,byte[] d,int renew,int rebind,int lease)
    {
        try 
        {
            this.server = new DatagramSocket(67);
        } 
        catch (SocketException ex) 
        {
            ex.printStackTrace();
        }
        this.gateway = g;
        this.subnetmask = s;
        this.dns = d;
        this.renewaltime = renew;
        this.rebindingtime = rebind;
        this.leasetime = lease;
        this.ip = new Vector<byte[]>();
        this.mac = new Vector<byte[]>();
        this.starttime = new Vector<Long>();
        this.reservedip = new Vector<byte[]>();
        this.reservedmac = new Vector<byte[]>();
        this.declinemac = new Vector<byte[]>();
        this.declineip = new Vector<byte[]>();
        this.message = new Vector<Message>();
        this.serverstart = System.currentTimeMillis() / 1000;
    }
    
    //Server Starter
    public void run()
    {
        while(true)
        {
            byte[] buf = new byte[2048];
            DatagramPacket receivepacket = new DatagramPacket(buf, buf.length);
            try
            {
                server.receive(receivepacket);
            }
            catch(IOException ex)
            {
                return;
            }
            Message receivemessage = new Message();
            receivemessage.receive(receivepacket.getData());
            message.add(receivemessage);
            if(!Arrays.equals(receivemessage.getMagiccookie(), new byte[]{(byte)99,(byte)130,(byte)83,(byte)99}))
                continue;
            int type = 0; 
            for(int i = 0 ; i < receivemessage.getOptions().size() ; i++)
            {
                if(receivemessage.getOptions().elementAt(i).getId() == 53)
                {
                    type = receivemessage.getOptions().elementAt(i).getValue()[0];
                    break;
                }
            }
            switch(type)
            {
                //Discover
                case 1:
                    Offer(receivemessage);
                    break;
                //Request
                case 3:
                    AckNack(receivemessage);
                    break;
                //Decline
                case 4:
                    Decline(receivemessage);
                    break;
                //Release    
                case 7:
                    Release(receivemessage);
                    break;
                //Inform
                case 8:
                    Inform(receivemessage);
                    break;
            }
        }
    }
    
    //Server Stopper
    public void stop()
    {
        this.server.close();
    }
    
    //Normal Offer
    private void Offer(Message m)
    {
        Random r = new Random();
        ClearReserved();
        CheckTime();
        if(this.ip.size() == 254)
            return;
        Message send = new Message();
        send.setOp(new byte[]{(byte)2});
        send.setHtype(m.getHtype());
        send.setHlen(m.getHlen());
        send.setHops(new byte[]{0});
        send.setXid(m.getXid());
        send.setFlags(new byte[]{(byte)128,0});
        byte[] newip = null;
        while(true)
        {
            newip = new byte[]{(byte)192,(byte)168,(byte)1,(byte)(r.nextInt(255) + 1)};
            if(!VectorSearch(this.ip, newip))
                if(!VectorSearch(this.reservedip, newip))
                {
                    if(VectorSearch(this.declinemac, Arrays.copyOfRange(m.getChaddr(),  0, 6)))
                    {
                        if(Arrays.equals(this.declineip.elementAt(IndexOf(this.declinemac, Arrays.copyOfRange(m.getChaddr(),  0, 6))), newip))
                            continue;
                    }
                    this.reservedmac.add(Arrays.copyOfRange(m.getChaddr(),  0, 6));
                    this.reservedip.add(newip);
                    break;
                }
        }
        send.setYiaddr(newip);
        send.setSiaddr(new byte[]{(byte)192,(byte)168,(byte)1,(byte)1});
        send.setChaddr(m.getChaddr());
        send.setMagiccookie(m.getMagiccookie());
        //Type
        Option messagetype = new Option((byte)1);
        messagetype.setId((byte)53);
        messagetype.setSize((byte)1);
        messagetype.setValue(new byte[]{(byte)2});
        send.getOptions().add(messagetype);
        
        OptionAdder(send , 1);
        message.add(send);
        
        try
        {
            InetAddress address = InetAddress.getByAddress(new byte[]{(byte)255,(byte)255,(byte)255,(byte)255});
            DatagramPacket p = new DatagramPacket(send.send(), send.send().length, address, 68);
            server.send(p);
        }
        catch(UnknownHostException ex)
        {
            ex.printStackTrace();
        }
        catch(IOException ex)
        {
            ex.printStackTrace();
        }
    }
    
    //Ack Or Nack
    private void AckNack(Message m)
    {
        Message send = new Message();
        send.setOp(new byte[]{(byte)2});
        send.setHtype(m.getHtype());
        send.setHlen(m.getHlen());
        send.setHops(new byte[]{0});
        send.setXid(m.getXid());
        send.setFlags(new byte[]{(byte)128,0});
        int type = 0;
        boolean nob = false;
        if(VectorSearch(this.mac, Arrays.copyOfRange(m.getChaddr(),  0, 6)))
        {
            nob = true;
            int index = IndexOf(this.mac, Arrays.copyOfRange(m.getChaddr(),  0, 6));
            this.starttime.setElementAt(System.currentTimeMillis() / 1000, index);
            send.setYiaddr(this.ip.elementAt(index));
            type = 1;
            //Type
            Option messagetype = new Option((byte)1);
            messagetype.setId((byte)53);
            messagetype.setSize((byte)1);
            messagetype.setValue(new byte[]{(byte)5});
            send.getOptions().add(messagetype);
        }
        else
        {
            if(VectorSearch(this.reservedmac, Arrays.copyOfRange(m.getChaddr(),  0, 6)))
            {
                int index = IndexOf(this.reservedmac, Arrays.copyOfRange(m.getChaddr(),  0, 6));
                if(Arrays.equals(this.reservedip.elementAt(index), GetRequestedIP(m)))
                {
                    this.reservedmac.remove(index);
                    this.reservedip.remove(IndexOf(this.reservedip, GetRequestedIP(m)));
                    this.mac.add(Arrays.copyOfRange(m.getChaddr(),  0, 6));
                    this.ip.add(GetRequestedIP(m));
                    this.starttime.add(System.currentTimeMillis() / 1000);
                    send.setYiaddr(GetRequestedIP(m));
                    type = 1;
                    //Type
                    Option messagetype = new Option((byte)1);
                    messagetype.setId((byte)53);
                    messagetype.setSize((byte)1);
                    messagetype.setValue(new byte[]{(byte)5});
                    send.getOptions().add(messagetype);
                }
                else
                {
                    this.reservedmac.remove(index);
                    this.reservedip.remove(IndexOf(this.reservedip, GetRequestedIP(m)));
                    send.setYiaddr(new byte[]{0,0,0,0});
                    //Type
                    Option messagetype = new Option((byte)1);
                    messagetype.setId((byte)53);
                    messagetype.setSize((byte)1);
                    messagetype.setValue(new byte[]{(byte)6});
                    send.getOptions().add(messagetype);
                }
            }
            else
            {
                send.setYiaddr(new byte[]{0,0,0,0});
                //Type
                Option messagetype = new Option((byte)1);
                messagetype.setId((byte)53);
                messagetype.setSize((byte)1);
                messagetype.setValue(new byte[]{(byte)6});
                send.getOptions().add(messagetype);
            }
        }
        send.setSiaddr(new byte[]{(byte)192,(byte)168,(byte)1,(byte)1});
        send.setChaddr(m.getChaddr());
        send.setMagiccookie(m.getMagiccookie()); 
        
        OptionAdder(send , type);
        message.add(send);
        
        try
        {
            InetAddress address;
            if(nob)
            {
                address = InetAddress.getByAddress(send.getYiaddr());
                send.setFlags(new byte[]{0,0});
            }
            else
                address = InetAddress.getByAddress(new byte[]{(byte)255,(byte)255,(byte)255,(byte)255});
            DatagramPacket p = new DatagramPacket(send.send(), send.send().length, address, 68);
            server.send(p);
        }
        catch(UnknownHostException ex)
        {
            ex.printStackTrace();
        }
        catch(IOException ex)
        {
            ex.printStackTrace();
        }
    }
    
    //Response Of Decline
    private void Decline(Message m)
    {
        int index = IndexOf(this.reservedmac, Arrays.copyOfRange(m.getChaddr(),  0, 6));
        this.declinemac.add(this.reservedmac.elementAt(index));
        this.declineip.add(this.reservedip.elementAt(index));
        this.reservedmac.remove(index);
        this.reservedip.remove(index);
    }
    
    //Release An Ip In Response Of Release Message
    private void Release(Message m)
    {
        int index = IndexOf(this.mac, Arrays.copyOfRange(m.getChaddr(),  0, 6));
        if(index == -1)
            return;
        this.mac.remove(index);
        this.ip.remove(index);
    }
    
    //Send Server Option In Resopsone Of Inform Message
    private void Inform(Message m)
    {
        Message send = new Message();
        send.setOp(new byte[]{(byte)2});
        send.setHtype(m.getHtype());
        send.setHlen(m.getHlen());
        send.setHops(new byte[]{0});
        send.setXid(m.getXid());
        send.setFlags(new byte[]{0,0});
        send.setYiaddr(this.ip.elementAt(IndexOf(this.mac, Arrays.copyOfRange(m.getChaddr(),  0, 6))));
        send.setSiaddr(new byte[]{(byte)192,(byte)168,(byte)1,(byte)1});
        send.setChaddr(m.getChaddr());
        send.setMagiccookie(m.getMagiccookie());
        //Type
        Option messagetype = new Option((byte)1);
        messagetype.setId((byte)53);
        messagetype.setSize((byte)1);
        messagetype.setValue(new byte[]{(byte)5});
        send.getOptions().add(messagetype);
        
        OptionAdder(send , 0);
        message.add(send);
        
        try
        {
            InetAddress address = InetAddress.getByAddress(send.getYiaddr());
            DatagramPacket p = new DatagramPacket(send.send(), send.send().length, address, 68);
            server.send(p);
        }
        catch(UnknownHostException ex)
        {
            ex.printStackTrace();
        }
        catch(IOException ex)
        {
            ex.printStackTrace();
        }
    }
    
    //Check The Times
    private void CheckTime()
    {
        Vector<Integer>remove = new Vector<Integer>();
        for(int i = 0 ; i < this.starttime.size() ; i++)
        {
            if((System.currentTimeMillis() / 1000) > this.starttime.elementAt(i) + this.leasetime)
                remove.add(new Integer(i));
        }
        for(int j = 0 ; j < remove.size() ; j++)
        {
            this.mac.remove((int)remove.elementAt(j));
            this.ip.remove((int)remove.elementAt(j));
            this.starttime.remove((int)remove.elementAt(j));
        }
    }
    
    //Clear The Reserverd IP Vector And Reserved Mac Vector
    private void ClearReserved()
    {
        if(System.currentTimeMillis() / 1000 > this.serverstart + 300)
        {
            this.reservedmac.clear();
            this.reservedip.clear();
            this.serverstart = System.currentTimeMillis() / 1000;
        }
    }
        
    //Conver An Integer To Array Of Bytes
    private byte[] IntToByte(int number)
    {
        byte[] result = new byte[4];
        for(int i = 0 ; i < 4 ; i++)
        {
            int offset = (result.length - 1 - i) * 8;
            result[i] = (byte)((number >>> offset) & 0xFF);
        }
        return result;
    }
    
    //Get Value Of Option 50
    public byte[] GetRequestedIP(Message m)
    {
        byte[] result = new byte[4];
        for(int i = 0 ; i < m.getOptions().size() ; i++)
            {
                if(m.getOptions().elementAt(i).getId() == 50)
                {
                    result = m.getOptions().elementAt(i).getValue();
                    break;
                }
            }
        return result;
    }
    
    //Search For An Array Of Bytes In A Vector
    private boolean VectorSearch(Vector<byte[]> v, byte[] array)
    {
        boolean result = false;
        if(v.isEmpty())
            return result;
        for(int i = 0 ; i < v.size() ; i++)
        {
            result = Arrays.equals(v.elementAt(i), array);
            if(result)
                break;
        }
        return result;
    }
    
    //Find The Index Of An Array OF Bytes In A Vector
    private int IndexOf(Vector<byte[]> v, byte[] array)
    {
        int result = -1;
        for(int i = 0 ; i < v.size() ; i++)
        {
            if(Arrays.equals(v.elementAt(i), array))
            {
                result = i;
                break;
            }
        }
        return result;
    }
    
    private void OptionAdder(Message me , int type)
    {
        //Gateway
        Option messagegateway = new Option((byte)4);
        messagegateway.setId((byte)3);
        messagegateway.setSize((byte)4);
        messagegateway.setValue(this.gateway);
        me.getOptions().add(messagegateway); 
        //Subnet mask
        Option subnet = new Option((byte)4);
        subnet.setId((byte)1);
        subnet.setSize((byte)4);
        subnet.setValue(this.subnetmask);
        me.getOptions().add(subnet);
        //DNS
        Option messageDNS = new Option((byte)4);
        messageDNS.setId((byte)6);
        messageDNS.setSize((byte)4);
        messageDNS.setValue(this.dns);
        me.getOptions().add(messageDNS);
        //Server IP
        Option serverip = new Option((byte)4);
        serverip.setId((byte)54);
        serverip.setSize((byte)4);
        serverip.setValue(new byte[]{(byte)192,(byte)168,(byte)1,(byte)1});
        me.getOptions().add(serverip); 
        if(type == 1)
        {
            //Renewal Time
            Option messagerenewaltime = new Option((byte)4);
            messagerenewaltime.setId((byte)58);
            messagerenewaltime.setSize((byte)4);
            messagerenewaltime.setValue(IntToByte(this.renewaltime));
            me.getOptions().add(messagerenewaltime); 
            //Rebinding Time
            Option messageribindingtime = new Option((byte)4);
            messageribindingtime.setId((byte)59);
            messageribindingtime.setSize((byte)4);
            messageribindingtime.setValue(IntToByte(this.rebindingtime));
            me.getOptions().add(messageribindingtime);
            //Lease Time
            Option messageleasetime = new Option((byte)4);
            messageleasetime.setId((byte)51);
            messageleasetime.setSize((byte)4);
            messageleasetime.setValue(IntToByte(this.leasetime));
            me.getOptions().add(messageleasetime);
        }
    }
    
    public Vector<byte[]> getIP()
    {
        return this.ip;
    }
    public Vector<byte[]> getmac()
    {
        return this.mac;
    }
    public Vector<Message> getMessage()
    {
        return this.message;
    }
}