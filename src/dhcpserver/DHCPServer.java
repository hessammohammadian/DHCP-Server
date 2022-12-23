package dhcpserver;

public class DHCPServer 
{
    public static void main(String[] args) 
    {
        java.awt.EventQueue.invokeLater(new Runnable() 
                {
                    public void run() 
                    {
                        new DHCP().setVisible(true);
                    }
                });
    }
}