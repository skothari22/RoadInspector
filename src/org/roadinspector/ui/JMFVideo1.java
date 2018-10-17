/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.roadinspector.ui;

import javax.swing.*;
import javax.media.*;
import java.net.*;
import java.io.*;


public class JMFVideo1
{
     public static void main(String args[]) throws Exception
     {
          Player player;
          JFrame jframe=new JFrame();
          jframe.setSize(300,300);
          JPanel jpanel=new JPanel();
          jpanel.setLayout(new BoxLayout(jpanel,BoxLayout.Y_AXIS));
          player=Manager.createRealizedPlayer(new MediaLocator(new File("G:/java files1/jmf/love.mpg").toURL()));
          player.setMediaTime(new Time(30.0));
          jpanel.add(player.getVisualComponent());
          Control controls[]=player.getControls();
          CachingControl cc=null;
          for(int i=0;i<controls.length;i++)
          {
               if(controls[i] instanceof CachingControl)
               {
                    cc=(CachingControl) controls[i];

                    jpanel.add(cc.getProgressBarComponent());     

                    System.out.println("Found CachingControl");

               }

          }

          if(player.getControlPanelComponent()==null)

          {

               System.out.println("Null");

          }

          else

          {

               jpanel.add(player.getControlPanelComponent());

          }

          jframe.setContentPane(jpanel);

          jframe.setVisible(true);

          player.start();

     }

} 