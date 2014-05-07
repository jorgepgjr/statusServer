package br.com.statusServer;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.Properties;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;

public class StatusServer extends JPanel {

    private static final long serialVersionUID = 8065280897234256299L;
    
    boolean conectionFail = false;
    JLabel jbdev11 = new JLabel("JBDEV11: ");
    JLabel status11 = new JLabel(" ");
    JLabel jbdev23 = new JLabel("JBDEV23: ");       
    JLabel status23 = new JLabel(" ");
    JLabel jbdev05 = new JLabel("JBDEV05: ");       
    JLabel status05 = new JLabel(" ");
    
    public StatusServer(){
        setLayout(new BorderLayout());
        this.setComponentOrientation(java.awt.ComponentOrientation.LEFT_TO_RIGHT);

        final JLabel imagemPresuntinho = new JLabel(new ImageIcon(getClass().getResource("presuntinho.jpg")));
        final JButton button = new JButton("Refresh");
        button.setPreferredSize(new Dimension(400,40));

        button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (!conectionFail) {
                    carregaServerFiles();                    
                }
            };
        });
        
        JPanel details = new JPanel(new GridLayout(2,2));
        add(button, BorderLayout.PAGE_START);
        add(imagemPresuntinho, BorderLayout.LINE_START);
        
        details.add(jbdev11);
        details.add(status11);      
        details.add(jbdev23);
        details.add(status23);
        add(details, BorderLayout.CENTER);        
    }
	
	private void carregaStatus(String fileName, JLabel status) {    
        File fXmlFile = new File(fileName);
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder;
        Document doc = null;
        try {
            dBuilder = dbFactory.newDocumentBuilder();
            doc = dBuilder.parse(fXmlFile);
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        doc.getDocumentElement().normalize();
        NodeList nList = doc.getElementsByTagName("xa-datasource");
        Node nNode = nList.item(0);
        Element eElement = (Element) nNode;
        String url = eElement.getElementsByTagName("xa-datasource-property").item(0).getTextContent();
        if (url.equals("jdbc:oracle:thin:@172.20.5.140:1521:HOM10G03")){
            status.setText("Homolog");
        }else if (url.equals("jdbc:oracle:thin:@10.130.8.29:1521:HOM10G04")){
            status.setText("Amazon");
        }
    }
	
	protected void deletaTempFiles(){
	    try{
            new File("jbdev11.xml").delete();
        }catch(Exception e){
            e.printStackTrace();
        }
	    
	    try{
	        new File("jbdev23.xml").delete();
        }catch(Exception e){
            e.printStackTrace();
        }
	}

    protected void carregaServerFiles() {
        JSch jsch = new JSch();
        Session session = null;        
        String username = Constants.userDesenv;
        String hostname = Constants.ipDesenv;
        String password = Constants.passwordDesenv; 
        
        try {
            session = jsch.getSession(username, hostname, 22);            
            Properties config = new Properties();
            config.setProperty("StrictHostKeyChecking", "no");
            session.setConfig(config);
            session.setPassword(password);
            session.connect();

            Channel channel = session.openChannel("sftp");
            channel.connect();
            ChannelSftp sftpChannel = (ChannelSftp) channel;

            final String originFile = "/app/jboss/jboss-eap-5.2/jboss-as/server/JBDEV11/deploy/datasource/oracle-sac_homolog-xa-ds.xml";
            sftpChannel.get(originFile, "jbdev11.xml");
            carregaStatus("jbdev11.xml", status11);
            
            final String originFile23 = "/app/jboss/jboss-eap-5.2/jboss-as/server/JBDEV23/deploy/datasource/oracle-sac_homolog-amazon-xa-ds.xml";
            sftpChannel.get(originFile23, "jbdev23.xml");
            carregaStatus("jbdev23.xml", status23);
            
            final String originFile05 = "/app/jboss/jboss-4.3.0.GA_CP10/jboss-as/server/JBDEV05/deploy/datasource/oracle-assineabril_homolog-xa-ds.xml";
            sftpChannel.get(originFile05, "jbdev05.xml");
            //TODO: fazer o status dos serviços
            carregaStatus("jbdev23.xml", status05);
            
            sftpChannel.exit();
            session.disconnect();
        } catch (JSchException e) {
            // Não conseguiu se conectar por alguma razão, não deixar o presuntinho ficar clicando feito um louco
            conectionFail = true;
            e.printStackTrace();  
        } catch (SftpException e) {
            e.printStackTrace();
        }
        deletaTempFiles();        
    }
    
    /** Returns an ImageIcon, or null if the path was invalid. */
    protected static ImageIcon createImageIcon(String path,
                                               String description) {
        java.net.URL imgURL = StatusServer.class.getResource(path);
        if (imgURL != null) {
            return new ImageIcon(imgURL, description);
        } else {
            System.err.println("Couldn't find file: " + path);
            return null;
        }
    }
    
    /**
     * Create the GUI and show it.  For thread safety,
     * this method should be invoked from the
     * event dispatch thread.
     */
    private static void createAndShowGUI() {
        //Create and set up the window.
        JFrame frame = new JFrame("Status Servers DEV");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
 
        //Add content to the window.
        frame.add(new StatusServer());
 
        //Display the window.
        frame.pack();
        frame.setVisible(true);
    }
 
    public static void main(String[] args) {
        //Schedule a job for the event dispatch thread:
        //creating and showing this application's GUI.
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
        //Turn off metal's use of bold fonts
            UIManager.put("swing.boldMetal", Boolean.FALSE);
                 
        createAndShowGUI();
            }
        });
    }

}
