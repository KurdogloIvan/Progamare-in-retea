package com.company;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.Semaphore;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {
    final static String[] str = new String[86];
    public static void main(String[] args) throws Exception {
        Socket socket = new Socket("unite.md", 80);
        PrintWriter out = new PrintWriter(socket.getOutputStream(),true);
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        // отправляем HTTP запрос на сервер
        out.println("GET / HTTP/1.1");
        out.println("Host: unite.md:80");
        out.println("Accept: image");
        out.println("Accept-Language: en");
         // out.println("Content-Length: 16192");
        //   out.println("Accept-Encoding: compress, gzip");
        out.println("Connection:Close");
        out.println();
        // читаем ответ
        BufferedReader breader = new BufferedReader(in);
        StringBuilder sb = new StringBuilder(8096);
        int io = 0;
        while (io != -1) {
            io = breader.read();
            sb.append((char) io);
        }
        out.close();
        in.close();
        socket.close();
        String s = sb.toString();
        Pattern pattern = Pattern.compile("(http:)*([/|.|\\w|\\s|-])*\\.(?:jpg|gif|png)");
        Matcher matcher = pattern.matcher(s);

        int i=0;
        while (matcher.find()) {
            str[i] = s.substring(matcher.start(), matcher.end());
            System.out.println(str[i]);
            i++;
        }

        Semaphore sem = new Semaphore(4);
        int k=0;
        int j=0;
        while(k<str.length) {
            new Thread(new Download(sem,k,j)).start();
            k++;
            j++;
        }
    }

    static class Download implements Runnable {
        Semaphore sem; // семафор
        int k;
        int j;

        Download(Semaphore sem,int k,int j) {
            this.sem = sem;
            this.k = k;
            this.j = j;
        }

        public void run() {
            try {

                sem.acquire();
                downloadFiles(str[k], "Uimages/" + j + ".jpg");
                sem.release();

            } catch (InterruptedException e) {
                System.out.println("Программа сломана");
            }
        }
    }

    public static void downloadFiles(String filename, String strPath)  {
        try {
            Socket socket = new Socket("unite.md", 80);
            DataOutputStream bw = new DataOutputStream(socket.getOutputStream());
            bw.writeBytes("GET "+filename+" HTTP/1.1\r\n");
            bw.writeBytes("Host: unite.md:80\r\n\r\n");
            bw.flush();
            DataInputStream in = new DataInputStream(socket.getInputStream());
            OutputStream dos = new FileOutputStream(strPath);
            boolean headerEnded = false;

            byte[] bytes = new byte[2048];
            int length;
            while ((length = in.read(bytes)) != -1) {
                // если конец хедера получен, то записываем байты в файл как надо
                if (headerEnded)
                    dos.write(bytes, 0, length);
                    // проверяем последние 4 байта, и, если это конец хедера(\r\n\r\n, что в числовом представлении равно 13 10 13 10)
                    // то ставим флаг и записываем байты до конца указанной длины массива
                else {
                    for (int i = 0; i < 2045; i++) {
                        if (bytes[i] == 13 && bytes[i + 1] == 10 && bytes[i + 2] == 13 && bytes[i + 3] == 10) {
                            headerEnded = true;
                            for(int j=i+4; j< 2045; j++) {
                                if(bytes[j] !=0) {
                                    dos.write(bytes, j, 2048 - i - 4);
                                    break;
                                }
                            }
                        }
                    }
                }
            }
            in.close();
            dos.close();
            socket.close();
        } catch (Exception e) {
            System.out.println(e);
        }
    }
}