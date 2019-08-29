package com.example.smstts;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.ToggleButton;

import org.w3c.dom.Text;

public class MainActivity extends Activity {

    private  final int CHECK_CODE = 0x1;
    private  final int LONG_DURATION =5000;
    private  final int SHORT_DURATION =1200;

    private Speaker speaker;

    private ToggleButton toggle;
    private CompoundButton.OnCheckedChangeListener toggleListener;
    private TextView smsText;
    private TextView smsSender;

    private BroadcastReceiver smsReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        toggle = (ToggleButton) findViewById(R.id.speechToggle);
        smsText = (TextView) findViewById(R.id.sms_text);
        smsSender = (TextView) findViewById(R.id.sms_sender);

        toggleListener = new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    speaker.allow(true);
                    speaker.speak("메세지 읽기를 시작합니다");
                } else {
                    speaker.speak("중단합니다");
                    speaker.allow(false);
                }
            }
        };

        toggle.setOnCheckedChangeListener(toggleListener);

        checkTTS();
        initializeSMSReceiver();
        registerSmsReceiver();

    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(smsReceiver);
        speaker.destroy();
    }


    private void checkTTS() {
        Intent check = new Intent();
        check.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
        startActivityForResult(check, CHECK_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        if (requestCode == CHECK_CODE) {
            if(resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS){
                speaker = new Speaker(this);
            }else {
                Intent install = new Intent();
                install.setAction(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
                startActivity(install);

            }
        }
    }

    private void initializeSMSReceiver() {
        smsReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Bundle bundle = intent.getExtras();
                if(bundle != null){// 번들에 포함된 문자 데이터를 객체 배열로 받아온다
                    Object [] pdus = (Object[]) bundle.get("pdus"); // SMS를 받아올 SmsMessage 배열을 만든다
                    for(int i = 0; i < pdus.length; i++){
                        // SmsMessage의 static메서드인 createFromPdu로 pdusObj의
                        // 데이터를 message에 담는다
                        // 이 때 pdusObj는 byte배열로 형변환을 해줘야 함
                        byte[] pdu = (byte[]) pdus[i];
                        SmsMessage message = SmsMessage.createFromPdu(pdu);
                        String text = message.getDisplayMessageBody();
                        String sender = getContactName(message.getOriginatingAddress());
                        speaker.pause(LONG_DURATION);
                        speaker.speak(" 새메세지가 " + sender + "에게 왔습니다");
                        speaker.pause(SHORT_DURATION);
                        speaker.speak(text);
                        smsSender.setText("보낸사람은" + sender);
                        smsText.setText(text);

                    }
                }

            }
        };
    }
    private String getContactName(String phone){
        Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(phone));
        String projection[] = new String[]{ContactsContract.Data.DISPLAY_NAME};
        Cursor cursor = getContentResolver().query(uri, projection, null,null,null);
        if(cursor.moveToFirst()){
            return cursor.getString(0);
        } else {
            return "알수없는 번호";
        }

    }

    private void registerSmsReceiver() {
        IntentFilter intentFilter = new IntentFilter("android.provider.Telephony.SMS_RECEIVED");
        registerReceiver(smsReceiver,intentFilter);
    }


}
