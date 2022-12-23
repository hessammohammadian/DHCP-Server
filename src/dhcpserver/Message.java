package dhcpserver;

import java.util.Vector;
import java.util.Arrays;
public class Message 
{
    private byte[] op;
    private byte[] htype;
    private byte[] hlen;
    private byte[] hops;
    private byte[] xid;
    private byte[] secs;
    private byte[] flags;
    private byte[] ciaddr;
    private byte[] yiaddr;
    private byte[] siaddr;
    private byte[] giaddr;
    private byte[] chaddr;
    private byte[] sname;
    private byte[] file;
    private byte[] magiccookie;
    private Vector<Option>options;
    
    public Message()
    {
        this.op = new byte[1];
        this.htype = new byte[1];
        this.hlen = new byte[1];
        this.hops = new byte[1];
        this.xid = new byte[4];
        this.secs = new byte[2];
        for(int i = 0 ; i < 2 ; i++)
            this.secs[i] = 0;
        this.flags = new byte[2];
        this.ciaddr = new byte[4];
        for(int i = 0 ; i < 4 ; i++)
            this.ciaddr[i] = 0;
        this.yiaddr = new byte[4];
        this.siaddr = new byte[4];
        this.giaddr = new byte[4];
        for(int i = 0 ; i < 4 ; i++)
            this.giaddr[i] = 0;
        this.chaddr = new byte[16];
        this.sname = new byte[64];
        for(int i = 0 ; i < 64 ; i++)
            this.sname[i] = 0;
        this.file = new byte[128];
        for(int i = 0 ; i < 128 ; i++)
            this.file[i] = 0;
        this.magiccookie = new byte[4];
        this.options = new Vector<Option>();
    }
    
    //set mathods
    public void setOp(byte[] o)
    {
        this.op = o;
    }
    public void setHtype(byte[] ht)
    {
        this.htype = ht;
    }
    public void setHlen(byte[] hl)
    {
        this.hlen = hl;
    }
    public void setHops(byte[] ho)
    {
        this.hops = ho;
    }
    public void setXid(byte[] xi)
    {
        this.xid = xi;
    }
    public void setSecs(byte[] se)
    {
        this.secs = se;
    }
    public void setFlags(byte[] fl)
    {
        this.flags = fl;
    }
    public void setCiaddr(byte[] ci)
    {
        this.ciaddr = ci;
    }
    public void setYiaddr(byte[] yi)
    {
        this.yiaddr = yi;
    }
    public void setSiaddr(byte[] si)
    {
        this.siaddr = si;
    }
    public void setGiaddr(byte[] gi)
    {
        this.giaddr = gi;
    }
    public void setChaddr(byte[] ch)
    {
        this.chaddr = ch;
    }
    public void setSname(byte[] sn)
    {
        this.sname = sn;
    }
    public void setFile(byte[] f)
    {
        this.file = f;
    }
    public void setMagiccookie(byte[] m)
    {
        this.magiccookie = m;
    } 
    
    //get methods
    public byte[] getOp()
    {
        return this.op;
    }
    public byte[] getHtype()
    {
        return this.htype;
    }
    public byte[] getHlen()
    {
        return this.hlen;
    }
    public byte[] getHops()
    {
        return this.hops;
    }
    public byte[] getXid()
    {
        return this.xid;
    }
    public byte[] getSecs()
    {
        return this.secs;
    }
    public byte[] getFlags()
    {
        return this.flags;
    }
    public byte[] getCiaddr()
    {
        return this.ciaddr;
    }
    public byte[] getYiaddr()
    {
        return this.yiaddr;
    }
    public byte[] getSiaddr()
    {
        return this.siaddr;
    }
    public byte[] getGiaddr()
    {
        return this.giaddr;
    }
    public byte[] getChaddr()
    {
        return this.chaddr;
    }
    public byte[] getSname()
    {
        return this.sname;
    }
    public byte[] getFile()
    {
        return this.file;
    }
    public byte[] getMagiccookie()
    {
        return this.magiccookie;
    }
    public  Vector<Option> getOptions()
    {
        return this.options;
    }
    
    //make a message from an array of bytes
    public void receive(byte[] receivepacket)
    {
        this.setOp(Arrays.copyOfRange(receivepacket, 0, 1));//recieve Op
        this.setHtype(Arrays.copyOfRange(receivepacket, 1, 2));//recieve Htype
        this.setHlen(Arrays.copyOfRange(receivepacket, 2, 3));//recieve Hlen
        this.setHops(Arrays.copyOfRange(receivepacket, 3, 4));//recieve Hops
        this.setXid(Arrays.copyOfRange(receivepacket, 4, 8));//recieve Xid
        this.setSecs(Arrays.copyOfRange(receivepacket, 8, 10));//recieve Secs
        this.setFlags(Arrays.copyOfRange(receivepacket, 10, 12));//recieve Flags
        this.setYiaddr(Arrays.copyOfRange(receivepacket, 16, 20));//recieve Yiaddr
        this.setSiaddr(Arrays.copyOfRange(receivepacket, 20, 24));//recieve Siaddr
        this.setChaddr(Arrays.copyOfRange(receivepacket, 28, 44));//recieve Chaddr
        this.setMagiccookie(Arrays.copyOfRange(receivepacket, 236, 240));//recieve Magic Cookie
        //recieve Options
        int i = 240;
        while(i < receivepacket.length)
        {
            byte m = (byte)255;
            if(receivepacket[i] == m)
                break;
            if(receivepacket[i] == 53 || receivepacket[i] == 50)
            {
                Option temp = new Option(receivepacket[i + 1]);
                temp.setId(receivepacket[i]);
                temp.setSize(receivepacket[i + 1]);
                temp.setValue(Arrays.copyOfRange(receivepacket, i + 2, i + temp.getSize() + 2));
                this.options.add(temp);
                i = i + temp.getSize() + 2;
            }
            else
                i = i + receivepacket[i+1] + 2;
        }
    }
    
    //make an array of bytes from a messge
    public byte[] send()
    {
        //compute size of array
        int size = this.getOptions().size() * 2;
        for(int i = 0 ; i < this.getOptions().size() ; i++)
            size += this.getOptions().elementAt(i).getSize();
        byte[] out = new byte[size + 241];
        out[0] = this.getOp()[0];//set Op
        out[1] = this.getHtype()[0];//set Htype
        out[2] = this.getHlen()[0];//set Hlen
        out[3] = this.getHops()[0];//set Hops
        //set Xid
        int x = 0;
        for(int i = 4 ; i < 8 ; i++)
        {
            out[i] = this.getXid()[x];
            x++;
        }
        //set Secs
        out[8] = 0;
        out[9] = 0;
        //set Flags
        out[10] = this.getFlags()[0];
        out[11] = this.getFlags()[1];
        //set Ciaddr
        x = 0;
        for(int i = 12 ; i < 16 ; i++)
        {
            out[i] = this.getCiaddr()[x];
            x++;
        }
        //set Yiaddr
        x = 0;
        for(int i = 16 ; i < 20 ; i++)
        {
            out[i] = this.getYiaddr()[x];
            x++;
        }
        //set Siaddr
        x = 0;
        for(int i = 20 ; i < 24 ; i++)
        {
            out[i] = this.getSiaddr()[x];
            x++;
        }
        //set Giaddr
        x = 0;
        for(int i = 24 ; i < 28 ; i++)
        {
            out[i] = this.getGiaddr()[x];
            x++;
        }
        //set Chaddr
        x = 0;
        for(int i = 28 ; i < 44 ; i++)
        {
            out[i] = this.getChaddr()[x];
            x++;
        }
        //set Sname
        x = 0;
        for(int i = 44 ; i < 108 ; i++)
        {
            out[i] = this.getSname()[x];
            x++;
        }
        //set File
        x = 0;
        for(int i = 108 ; i < 236 ; i++)
        {
            out[i] = this.getFile()[x];
            x++;
        }
        //set Magic Cookie
        x = 0;
        for(int i = 236 ; i < 240 ; i++)
        {
            out[i] = this.getMagiccookie()[x];
            x++;
        }
        //set option
        int j = 240;
        for(int i = 0 ; i < this.getOptions().size() ; i++)
        {
            out[j] = this.getOptions().elementAt(i).getId();
            j++;
            out[j] = this.getOptions().elementAt(i).getSize();
            j++;
            x = 0;
            for(int m = 0 ; m < this.getOptions().elementAt(i).getSize() ; m++)
            {
                out[j] = this.getOptions().elementAt(i).getValue()[x];
                j++; 
                x++;
            }           
        }
        out[j] = (byte)255;
        
        return out;
    }
    
    public int messageType()
    {
        int type = 0;
        for(int i = 0 ; i < this.getOptions().size() ; i++)
        {
            if(this.getOptions().elementAt(i).getId() == 53)
                type = this.getOptions().elementAt(i).getValue()[0];
        }
        return type;
    }
}