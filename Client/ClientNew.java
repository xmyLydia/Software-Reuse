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
    //经过server判断之后登录成功的次数
    private int loginSuccess=0;
    //登录失败的次数
    private int loginFail=0;
    //发送的消息数量
    private int send_message=0;
    //从服务端收到的消息数量
    private int received_message=0;
    //用于每分钟存入文件的计时器
    private Timer SaveMsgTimer;
    //同步锁
    private static Object SendMsgLock = new Object();
    //写入文件
    private IOLog ClientLog;
    //用于存放登录的用户名和密码
    Map<String,String> map=new HashMap<String,String>();
    //客户端输入
    String input="";
    private String nameForFile="";
   
    /**
     * 与服务器连接，并输入发送消息
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
                //文件存储
                 if(!nameForFile.equals(""))
                {
                	//如果保存的用户名存在的话，进行计时器的创建
                	 String fileName=createFile(nameForFile);//使用username作为文件的名字
                     ClientLog = new IOLog(fileName, true);
                     SaveMsgTimer=new Timer();//计时器
                     SaveMsgTimer.schedule(new SaveRecord(), 0,60000);
                   
                }
              
            }
        }
    }
   
    //创建文件
    public String createFile(String lastname){
        File f = new File(".");
        // fileName表示你创建的文件名；为txt类型；
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

    //此类用于存入每分钟发送的消息数
    class SaveRecord extends TimerTask{
        @Override
        public void run() {
            // TODO Auto-generated method stub
            //将每分钟发送的消息数存入文件中
            SaveToFile() ;
        //    System.out.println("this is a timer"+nameForFile);
            
            synchronized (SendMsgLock) {
                send_message=0;
            }
        }
        public void SaveToFile(){
            //将每分钟发送的消息数存入文件中
            String res;
            res = new SimpleDateFormat("yyyyMMdd_HHmmss: ").format(Calendar.getInstance().getTime());
            ClientLog.IOWrite(res+"client send message : " +  send_message + "\n");
            ClientLog.IOWrite(res+"client receive message: " + received_message + "\n");
            ClientLog.IOWrite(res+"client login success: " + loginSuccess + "\n");
            ClientLog.IOWrite(res+"client login fail: " + loginFail + "\n");
        }
    }

    //处理登录
    public void loginClient() throws IOException{
        String line;
        Message msgClient;
        String username;
        String password;

        stdinFlag = false;

        while (true) {
            try {
                System.out.print("please input the username：");
                username = strin.readLine();
                System.out.println("please input the password：");
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
                    
                    //提示用户登录成功
                    System.out.println("login successfully, please input the message:");
                    //将成功登录的用户名和密码存入Map
                    map.put(msgClient.getValue("username"), msgClient.getValue("password"));
                  //  map.put(username, password);
                    nameForFile=username;
                    //调用函数使得从客户端读取消息并且发送到服务端
                    break;
                }
                if (msgClient.getValue("event").equals("invalid")) {//登录失败
                    ++loginFail;
                    //提示用户登录失败
                    System.out.println("login failed, please login again");
                }
            } catch (JSONException e) {
                continue;
            }
        }

        stdinFlag = true;

        
    }

    /**
     * 用于监听服务器端向客户端发送消息线程类
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
                    if(msgClient.getValue("event").equals("quit")){//客户端申请退出，服务端返回确认退出
                        break;
                    } else if (msgClient.getValue("event").equals("login")) {
                        loginClient();
                        
                    } else if (msgClient.getValue("event").equals("relogin")) {
                        result = buff.readLine();
                        loginClient();
                    } else if (msgClient.getValue("event").equals("logedin")) {
                        System.out.println("user: "+msgClient.getValue("username")+" loged in.");
                    } else if (msgClient.getValue("event").equals("message")) { //输出服务端发送消息
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
            new ClientNew();//启动客户端
            
        }catch (Exception e) {
        }
    }
}