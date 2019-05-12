package Gui;

import javax.swing.*;
import java.awt.*;

public class Gui extends JFrame {
    private static final String START = "Старт";
    private static final String STOP = "Стоп";
    private static final String TITLE = "Торговый бот";

    public Gui() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setSize();
        setTitle(TITLE);
        JPanel jPanel = new JPanel();
        add(jPanel);
        JButton jButton = new JButton(START);
        jPanel.add(jButton);
        JScrollPane jScrollPane = new JScrollPane();
        JTextArea jTextArea = new JTextArea();
        jTextArea.setColumns(80);
        jTextArea.setRows(10);
        jScrollPane.setViewportView(jTextArea);
        jPanel.add(jScrollPane);

        BotStarter botStarter = new BotStarter();
        botStarter.addStateListener(state -> {
                    jButton.setText(state ? STOP : START);
                    jButton.setEnabled(true);
                }
        );

        botStarter.addRangePairListener(message -> {
                    String text = message + jTextArea.getText();
                    jTextArea.setText(text);
                }
        );


        jButton.addActionListener(e -> {
            switch (e.getActionCommand()) {
                case START:
                    jButton.setEnabled(false);
                    botStarter.start();
                    break;
                case STOP:
                    jButton.setEnabled(false);
                    botStarter.stop();
                    break;
            }
        });

    }

    private void setSize() {
        Toolkit toolkit = Toolkit.getDefaultToolkit();
        Dimension dimension = toolkit.getScreenSize();
        int width = 900;
        int height = 300;
        int right = dimension.width / 2 - width / 2;
        int top = dimension.height / 2 - height / 2;
        setBounds(right, top, width, height);
    }

}
