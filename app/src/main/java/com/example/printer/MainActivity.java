package com.example.printer;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.printer.databinding.ActivityMainBinding;
import com.google.android.material.snackbar.Snackbar;
import com.starmicronics.stario.PortInfo;
import com.starmicronics.stario.StarIOPort;
import com.starmicronics.stario.StarIOPortException;
import com.starmicronics.stario.StarPrinterStatus;
import com.starmicronics.starioextension.ICommandBuilder;
import com.starmicronics.starioextension.StarIoExt;

import java.util.List;


public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration appBarConfiguration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        com.example.printer.databinding.ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        appBarConfiguration = new AppBarConfiguration.Builder(navController.getGraph()).build();
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);

        binding.fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Printing", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();

                if (MainActivity.this.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED || MainActivity.this.checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_DENIED) {
                    requestPermissions(
                            new String[]{
                                    Manifest.permission.BLUETOOTH_CONNECT,
                                    Manifest.permission.BLUETOOTH_SCAN,
                            },
                            1000
                    );
                }
                String textToPrint =
                        "        Star Clothing Boutique\n" +
                                "             123 Star Road\n" +
                                "           City, State 12345\n" +
                                "\n" +
                                "Date:MM/DD/YYYY          Time:HH:MM PM\n" +
                                "--------------------------------------\n" +
                                "SALE\n" +
                                "SKU            Description       Total\n" +
                                "300678566      PLAIN T-SHIRT     10.99\n" +
                                "300692003      BLACK DENIM       29.99\n" +
                                "300651148      BLUE DENIM        29.99\n" +
                                "300642980      STRIPED DRESS     49.99\n" +
                                "30063847       BLACK BOOTS       35.99\n" +
                                "\n" +
                                "Subtotal                        156.95\n" +
                                "Tax                               0.00\n" +
                                "--------------------------------------\n" +
                                "Total                          $156.95\n" +
                                "--------------------------------------\n" +
                                "\n" +
                                "Charge\n" +
                                "156.95\n" +
                                "Visa XXXX-XXXX-XXXX-0123\n" +
                                "Refunds and Exchanges\n" +
                                "Within 30 days with receipt\n" +
                                "And tags attached\n";

                int      textSize = 25;
                Typeface typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL);

                Paint paint = new Paint();
                Bitmap bitmap;
                Canvas canvas;

                paint.setTextSize(textSize);
                paint.setTypeface(typeface);

                paint.getTextBounds(textToPrint, 0, textToPrint.length(), new Rect());

                TextPaint textPaint = new TextPaint(paint);
                android.text.StaticLayout staticLayout = new StaticLayout(textToPrint, textPaint, 576, Layout.Alignment.ALIGN_NORMAL, 1, 0, false);

                // Create bitmap
                bitmap = Bitmap.createBitmap(staticLayout.getWidth(), staticLayout.getHeight(), Bitmap.Config.ARGB_8888);

                // Create canvas
                canvas = new Canvas(bitmap);
                canvas.drawColor(Color.WHITE);
                canvas.translate(0, 0);
                staticLayout.draw(canvas);

                ICommandBuilder builder = StarIoExt.createCommandBuilder(StarIoExt.Emulation.StarGraphic);
                builder.beginDocument();
                builder.appendBitmap(bitmap, false);
                builder.appendCutPaper(ICommandBuilder.CutPaperAction.PartialCutWithFeed);
                builder.endDocument();
                byte[] commands = builder.getCommands();

                List<PortInfo> mPortList;
                StarIOPort port = null;
//                String portName;
                String portName = "BT:00:11:62:2E:65:1F";
//                String portName = "BT:Star Micronics";
                String portSettings = "";

                try
                {
//                    mPortList = StarIOPort.searchPrinter("BT:", MainActivity.this);
//                    portName = "BT:"+mPortList.get(0).getMacAddress();
                    System.out.println("Getting port: " + portName);
                    port = StarIOPort.getPort(portName, portSettings, 10000, MainActivity.this);

                    if (port == null) {
                        Toast.makeText(MainActivity.this, "No available port", Toast.LENGTH_SHORT).show();
                        throw new StarIOPortException("No port available");
                    }

                    System.out.println("begin checked block");
                    StarPrinterStatus status;
                    status = port.beginCheckedBlock();
                    if (status.offline) {
                        Toast.makeText(MainActivity.this, "Printer is offline", Toast.LENGTH_SHORT).show();
                        throw new StarIOPortException("Printer is offline");
                    }

                    System.out.println("printing: " + commands);
                    port.writePort(commands, 0, commands.length);

                    System.out.println("Waiting 30 seconds to check end block");
                    port.setEndCheckedBlockTimeoutMillis(30000);

                    System.out.println("end checked block");
                    status = port.endCheckedBlock();

                    System.out.println("checking status");
                    if (status.coverOpen) {
                        throw new StarIOPortException("Printer cover is open");
                    } else if (status.receiptPaperEmpty) {
                        throw new StarIOPortException("Receipt paper is empty");
                    } else if (status.offline) {
                        throw new StarIOPortException("Printer is offline");
                    }

                    System.out.println("done");
                }
                catch (StarIOPortException e)
                {
                   e.printStackTrace();
                }
                finally
                {
                    if(port != null)
                    {
                        try
                        {
                            System.out.println("releasing port");
                            StarIOPort.releasePort(port);
                        } catch (StarIOPortException e) {
                            e.printStackTrace();
                        }
                    } else {
                        System.out.println("no port to release");
                    }
                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
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

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, appBarConfiguration)
                || super.onSupportNavigateUp();
    }
}