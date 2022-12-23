package dhcpserver;

public class Option
{
    private byte id;
    private byte size;
    private byte[] value;
    
    public Option(byte size)
    {
        value = new byte[size];
    }
    public void setId(byte i)
    {
        this.id = i;
    }
    public void setSize(byte s)
    {
        this.size = s;
    }
    public void setValue(byte[] v)
    {
        this.value = v;
    }
    public byte getId()
    {
        return this.id;
    }
    public byte getSize()
    {
        return this.size;
    }
    public byte[] getValue()
    {
        return this.value;
    }
}