package com.ludonghua.robot;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;

public class PressKeyRobot {
    private Robot robot = null;

    public PressKeyRobot() {
        try {
            robot = new Robot();
        }
        catch (AWTException e) {
            // Auto-generated catch block
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        new PressKeyRobot().start();
    }

    public void start(){
        Project pro = new Project();
        Timer timer = new Timer();
        // 间隔时间5分钟
        timer.schedule(pro, Calendar.getInstance().getTime(), 60 * 5 * 1000);
    }


    //inner class
    class Project extends TimerTask {
        @Override
        public void run() {
            //自动按键 f5
            robot.keyPress(KeyEvent.VK_F5);
            robot.keyRelease(KeyEvent.VK_F5);
        }
    }
}
