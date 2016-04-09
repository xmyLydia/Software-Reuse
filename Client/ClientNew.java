package Client;

import org.json.JSONException;
import utils.IOLog;
import utils.Message;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by shieh on 3/21/16.
 */
public class ClientNew extends Socket {


    private static final String SERVER_IP ="127.0.0.1";
    private static final int SERVER_PORT =2095;
    private static Object loginLock = new Object();

    private Socket client;
    private PrintWriter out;
    private BufferedReader in;
    private BufferedReader strin;
    private final Object stdinLock = new Object();
    private boolean stdinFlag;
    //����server�ж�֮���¼�ɹ��Ĵ���
    private int loginSuccess=0;
    //��¼ʧ�ܵĴ���
    private int loginFail=0;
    //���͵���Ϣ����
    private int send_message=0;
    //�ӷ�����յ�����Ϣ����
    private int received_message=0;
    //����ÿ���Ӵ����ļ��ļ�ʱ��
    private Timer SaveMsgTimer;
    //ͬ����
    private static Object SendMsgLock = new Object();
    //д���ļ�
    private IOLog ClientLog;
    //���ڴ�ŵ�¼���û���������
    Map<String,String> map=new HashMap<String,String>();
    //�ͻ�������
    String input="";
    private String nameForFile="";
   
    /**
     * ����������ӣ������뷢����Ϣ
     */
    public  ClientNew()throws Exception{
        super(SERVER_IP, SERVER_PORT);
        client =this;
        Message msg;
        out =new PrintWriter(this.getOutputStream(),true);
        in =new BufferedReader(new InputStreamReader(this.getInputStream()));
        strin = new BufferedReader(new InputStreamReader(System.in));
        String input;
        readLineThread rt = new readLineThread();
        String getFromMap=map.get("username");
      
        stdinFlag = false;
        while(true){
            Thread.sleep(100);
            if (stdinFlag) {
                System.out.println("a");
                input = strin.readLine();
                msg = new Message("{}", 0);
                msg.setValue("msg", input);
                msg.setValue("event", "message");
                out.println(msg);
                getFromMap="hehe";
                //�ļ��洢
                 if(!nameForFile.equals(""))
                {
                	//���������û������ڵĻ������м�ʱ���Ĵ���
                	 String fileName=createFile(nameForFile);//ʹ��username��Ϊ�ļ�������
                     ClientLog = new IOLog(fileName, true);
                     SaveMsgTimer=new Timer();//��ʱ��
                     SaveMsgTimer.schedule(new SaveRecord(), 0,60000);
                   
                }
              
            }
        }
    }
   
    //�����ļ�
    public String createFile(String lastname){
        File f = new File(".");
        // fileName��ʾ�㴴�����ļ�����Ϊtxt���ͣ�
        String fileName=lastname+".txt";
        //String fileName="1234";
        File file = new File(f,fileName);
        if(!file.exists()){
            try {
                file.createNewFile();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        return fileName;
    }

    //�������ڴ���ÿ���ӷ��͵���Ϣ��
    class SaveRecord extends TimerTask{
        @Override
        public void run() {
            // TODO Auto-generated method stub
            //��ÿ���ӷ��͵���Ϣ�������ļ���
            SaveToFile() ;
        //    System.out.println("this is a timer"+nameForFile);
            
            synchronized (SendMsgLock) {
                send_message=0;
            }
        }
        public void SaveToFile(){
            //��ÿ���ӷ��͵���Ϣ�������ļ���
            String res;
            res = new SimpleDateFormat("yyyyMMdd_HHmmss: ").format(Calendar.getInstance().getTime());
            ClientLog.IOWrite(res+"client send message : " +  send_message + "\n");
            ClientLog.IOWrite(res+"client receive message: " + received_message + "\n");
            ClientLog.IOWrite(res+"client login success: " + loginSuccess + "\n");
            ClientLog.IOWrite(res+"client login fail: " + loginFail + "\n");
        }
    }

    //�����¼
    public void loginClient() throws IOException{
        String line;
        Message msgClient;
        String username;
        String password;

        stdinFlag = false;

        while (true) {
            try {
                System.out.print("please input the username��");
                username = strin.readLine();
                System.out.println("please input the password��");
                password = strin.readLine();
                msgClient = new Message("{}", 0);
                msgClient.setValue("event", "login");
                msgClient.setValue("username", username);
                msgClient.setValue("password", password);
                out.println(msgClient);
                line = in.readLine();
                msgClient = new Message(line, 0);
                if (msgClient.getValue("event").equals("valid")) {
                    ++loginSuccess;
                    
                    //��ʾ�û���¼�ɹ�
                    System.out.println("login successfully, please input the message:");
                    //���ɹ���¼���û������������Map
                    map.put(msgClient.getValue("username"), msgClient.getValue("password"));
                  //  map.put(username, password);
                    nameForFile=username;
                    //���ú���ʹ�ôӿͻ��˶�ȡ��Ϣ���ҷ��͵������
                    break;
                }
                if (msgClient.getValue("event").equals("invalid")) {//��¼ʧ��
                    ++loginFail;
                    //��ʾ�û���¼ʧ��
                    System.out.println("login failed, please login again");
                }
            } catch (JSONException e) {
                continue;
            }
        }

        stdinFlag = true;

        
    }

    /**
     * ���ڼ�������������ͻ��˷�����Ϣ�߳���
     */
    class readLineThread extends Thread{

        private BufferedReader buff;
        public readLineThread(){
            try {
                buff =new BufferedReader(new InputStreamReader(client.getInputStream()));
                start();
            }catch (Exception e) {
            }
        }

        @Override
        public void run() {
            Message msgClient;
            try {
                while(true){
                    String result = buff.readLine();
                    msgClient = new Message(result, this.getId());
                    System.out.println(msgClient);
                    if(msgClient.getValue("event").equals("quit")){//�ͻ��������˳�������˷���ȷ���˳�
                        break;
                    } else if (msgClient.getValue("event").equals("login")) {
                        loginClient();
                        
                    } else if (msgClient.getValue("event").equals("relogin")) {
                        result = buff.readLine();
                        loginClient();
                    } else if (msgClient.getValue("event").equals("logedin")) {
                        System.out.println("user: "+msgClient.getValue("username")+" loged in.");
                    } else if (msgClient.getValue("event").equals("message")) { //�������˷�����Ϣ
                        System.out.println(msgClient.getValue("username")+" said: "+msgClient.getValue("msg"));
                    }
                    synchronized (stdinLock) {
                        stdinLock.notify();
                    }
                }
                in.close();
                out.close();
                client.close();
            }catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        try {
            new ClientNew();//�����ͻ���
            
        }catch (Exception e) {
        }
    }
}