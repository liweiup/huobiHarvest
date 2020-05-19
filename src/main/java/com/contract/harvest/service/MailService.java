package com.contract.harvest.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;

@Service
public class MailService {

    @Value("${spring.mail.username}")
    private String fromMail;

    @Autowired
    private JavaMailSenderImpl mailSender;

    private static final String myReceiveMail = "321327476@qq.com";

    public void sendMail(String topic,String context,String receive_address) {
        String toMail = "";
        if (receive_address.equals("")) {
            toMail = myReceiveMail;
        }
        SimpleMailMessage simpleMailMessage =  new SimpleMailMessage();
        simpleMailMessage.setFrom(fromMail);
        simpleMailMessage.setSubject(topic);
        simpleMailMessage.setText(context);
        simpleMailMessage.setTo(toMail);
        mailSender.send(simpleMailMessage);
    }
}
