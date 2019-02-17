package com.example.sensei.troisieme;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Button;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import net.sourceforge.jFuzzyLogic.FIS;
import net.sourceforge.jFuzzyLogic.FunctionBlock;
import net.sourceforge.jFuzzyLogic.defuzzifier.Defuzzifier;
import net.sourceforge.jFuzzyLogic.rule.LinguisticTerm;
import net.sourceforge.jFuzzyLogic.rule.Variable;

import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;

import be.tarsos.dsp.util.fft.FFT;

public class MainActivity extends AppCompatActivity {

   // private static int sampleRate;

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }

    private static final int bufferSize = 20000;
    private static final byte[]buffer	= new byte[bufferSize];
    private static final int sampleRate = 44100;

    private final static int	enc     = AudioFormat.ENCODING_PCM_16BIT;
    private final static int	chan    = AudioFormat.CHANNEL_CONFIGURATION_MONO;
    private final static File file   = new File(Environment.getExternalStorageDirectory(),"troisime.wav");
    private final static int	length  = 1000;
    private double f;
    private String wght;
    private FIS fis;

    /** variable interface le izy **/

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //TextView result = (TextView) findViewById(R.id.result);

        Button record = (Button) findViewById(R.id.record);
        Button playback = (Button) findViewById(R.id.playback);
        Button logiq = (Button) findViewById(R.id.logiq);
       // traite   = (Button) findViewById(R.id.traite);

        record.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Thread() {
                    public void run() {
                        recordSound();
                    }
                }.start();
            }
        });

        playback.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Thread() {
                    public void run() {
                        traitement();
                    }
                }.start();
            }
        });

        logiq.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                new Thread() {
                    public void run() {
                        fuzzylogic();

                    }
                }.start();
                // text view
                TextView result = (TextView) findViewById(R.id.result);
                result.setText("Output weight is:"+wght+" ");

            }

        });

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    //traitement troisième
    public final  void traitement(){

        final int fftSize = bufferSize / 2;
        FFT fft = new FFT(bufferSize);
        final float[] amplitudes = new float[fftSize];
        float[] audioBuffer = new float[bufferSize];
        float[] essai = new float[bufferSize];

        for (int i = 0; i < bufferSize ; i++) {
            audioBuffer[i] = (float) ((double) buffer[i] / 32768.0); // signed 16 bit
             essai[i] = (float) ((double) buffer[i] / 32768.0);
        }
        fft.forwardTransform(audioBuffer);
        fft.modulus(audioBuffer, amplitudes);
        this.f=amplitudes[54];

        try{
            File text = new File (Environment.getExternalStorageDirectory(), "/te.txt");
            BufferedWriter out =new BufferedWriter(new FileWriter(text,true));
            // out = new PrintWriter(new FileWriter(text));
            out.write("delta Fe= 44100 " );
            out.newLine();
            out.write( "caracteristique " );
            out.write( "indices tab"+"frequence" +" amplitude" );
            out.newLine();
            for (int i = 0; i < amplitudes.length; i++) {
                double gh =  (int) fft.binToHz(i, sampleRate);
                out.write( String.valueOf(i)+" //  "+ String.valueOf(gh)+" hz est  de ");
                out.write(String.valueOf(amplitudes[i]));
                out.newLine();
                //out.write(String.valueOf(essai[i]));
                out.flush();
                //String.format("Amplitude at %3d Hz: %8.3f", (int) fft.binToHz(i, sampleRate) , amplitudes[i]);
            }
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //enregistrement
    private final static void recordSound(){
        Log.d("DEMO","RECORDING");
            try {
                final OutputStream os	= new FileOutputStream(file);
                final AudioRecord	ar	= new AudioRecord(MediaRecorder.AudioSource.MIC,sampleRate,chan,enc,bufferSize);
                saveWaveHeader(os);
                ar.startRecording();
                final long time = SystemClock.elapsedRealtime()+length;
                while(time>SystemClock.elapsedRealtime()){
                    ar.read(buffer, 0, bufferSize);
                    os.write(buffer);
                }
                ar.stop();
                os.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
    }

    //save la methode
    private final static void saveWaveHeader(OutputStream os) throws UnsupportedEncodingException, IOException {
        final DataOutputStream dos = new DataOutputStream(os);
        dos.write("RIFF".getBytes("ascii"));
        dos.writeInt(Integer.reverseBytes(32+length*sampleRate*2));
        dos.write("WAVE".getBytes("ascii"));
        dos.write("fmt ".getBytes("ascii"));
        dos.writeInt(Integer.reverseBytes(16));
        dos.writeShort(Short.reverseBytes((short)1));
        dos.writeInt(Integer.reverseBytes(sampleRate));
        dos.writeInt(Integer.reverseBytes(sampleRate*2));
        dos.writeShort(Short.reverseBytes((short)1));
        dos.writeShort(Short.reverseBytes((short)2));
        dos.writeShort(Short.reverseBytes((short)16));
        dos.write("data".getBytes("ascii"));
        dos.writeInt(Integer.reverseBytes(length*sampleRate*2));
    }

    // la logique floue l=est ici maintenant

    private final double fuzzylogic() {

        try {
            // Load from 'FCL' file
            InputStream inputStream = getAssets().open("tipper.fcl");
             this.fis = FIS.load(inputStream, true);



            // Error while loading?
            if (fis == null) {
                System.err.println("Can't load file: '" + inputStream + "'");
                return 0;
            }
            FunctionBlock test = fis.getFunctionBlock(null);

            // les variable a utilise la
            test.setVariable("hena", 6);
            test.setVariable("kisoa", 4);

            //sortit de logique
            test.evaluate();

            //double weight = test.getVariable("tip").getValue();
            Variable weight = fis.getVariable("weight");
            //double wght;
            wght =weight.toString();
//            Toast.makeText(MainActivity.this, "Output weight is:" + fis.getVariable("weight").getValue(), Toast.LENGTH_LONG).show();
            System.err.println("Output weight is:" + wght);

            return  0;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return 0;
    }





    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
