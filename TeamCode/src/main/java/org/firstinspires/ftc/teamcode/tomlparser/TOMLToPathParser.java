package org.firstinspires.ftc.teamcode.tomlparser;

import static org.firstinspires.ftc.robotcore.external.BlocksOpModeCompanion.hardwareMap;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Environment;

import com.acmerobotics.roadrunner.Pose2d;
import com.acmerobotics.roadrunner.TrajectoryActionBuilder;
import com.qualcomm.robotcore.util.WebHandlerManager;
//import com.acmerobotics.roadrunner.Vector2d;

import org.firstinspires.ftc.ftccommon.external.WebHandlerRegistrar;
import org.firstinspires.ftc.robotcore.internal.webserver.WebHandler;
import org.firstinspires.ftc.teamcode.MecanumDrive;
import org.tomlj.Toml;
import org.tomlj.TomlTable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import fi.iki.elonen.NanoHTTPD;

/**
 * TOML Parser method to use to avoid compiling every time a path is changed.
 *
 * @author Jude Naramor
 * @author Maulik Verma
 * @since May 2023
 * @version October 2023
 *
 */

public class TOMLToPathParser {
    ArrayList<TomlTable[]> tomlSequences = new ArrayList<>();

    @SuppressLint("NewApi")
//    public TOMLToPathParser() {
//        File dir = new File(".");
//        for (File file : dir.listFiles()) {
//            if(file.getName().endsWith(".toml")) {
//                tomlSequences.add(Toml.parse(file.getPath()).getTable("sequence");
//
//            }
//        }
//    }
    public TOMLToPathParser() {
        //gets the file
        String fsPath = String.format("%s/FIRST/toml/", Environment.getExternalStorageDirectory().getAbsolutePath());
        File tomlDir = new File(fsPath);
        tomlDir.mkdir();
        //Adds each file in the directory to a predetermined list of preparsed files to be used later
        for (File file : tomlDir.listFiles()) {
            //Need to connect the initial position of a file to the main sequences (weird quirk in how trajectory sequence and roadrunner works)
            tomlSequences.add(new TomlTable[]{Toml.parse(file.getPath()).getTable("sequence"), Toml.parse(file.getPath()).getTable("initialPosition")});
        }
    }

    @WebHandlerRegistrar
    public static void registerPaths(Context context, WebHandlerManager manager) {
        manager.register("/toml", new WebHandler() {
            @Override
            public NanoHTTPD.Response getResponse(NanoHTTPD.IHTTPSession session) throws IOException, NanoHTTPD.ResponseException {
                if (session.getMethod() != NanoHTTPD.Method.GET) return NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.BAD_REQUEST, NanoHTTPD.MIME_PLAINTEXT, "Get requests only");
                return NanoHTTPD.newChunkedResponse(NanoHTTPD.Response.Status.OK, NanoHTTPD.MIME_HTML, context.getAssets().open(""));
            }
        });
    }

    public TrajectoryActionBuilder Parse(String filename)
    {
        //Accesses the phone's file system
        File file = new File(filename);

        /*
        * If it alphabetical:
        * 0, 1 - first alpha
        * 2, 3 - second alpha
        * 2n, and 2n+1 as the two important files, with 2n being sequence and 2n+1 being initialPosition
        * */

        tomlSequences.add(new TomlTable[]{Toml.parse(file.getPath()).getTable("sequence"), Toml.parse(file.getPath()).getTable("initialPosition")});
        double lastHeading = (double) ((tomlSequences.get(1).getArray("initialPosition")).toList()).get(2);
        double lastX = (double) ((tomlSequences.get(1).getArray("initialPosition")).toList()).get(0);
        double lastY = (double) ((tomlSequences.get(1).getArray("initialPosition")).toList()).get(1);

        Pose2d startPose = new Pose2d(lastX, lastY, Math.toRadians(lastHeading));
        MecanumDrive drive = new MecanumDrive(hardwareMap, startPose);

        TrajectoryActionBuilder trajSeq = drive.actionBuilder(startPose);
        for (int i = 0; i < tomlSequences.get(0).getArray("sequence").toList().size() - 1; i++)
        {
            List list = (tomlSequences.get(0)).getArray("sequence.traj" + (i+1) + ".args").toList();
            double[] array = (double[]) list.get(0); // x, y

            //handle types of paths (i.e. constant, linear, spline, etc.
            switch  ((String)(tomlSequences.get(0)).getArray("sequence.traj" + (i+1) + ".args").toList().get(0)) {
                case "lineToLinearHeading":
                    trajSeq.lineToXLinearHeading(array[0], Math.toRadians((int)list.get(1)));
                    trajSeq.lineToYLinearHeading(array[1], Math.toRadians((int)list.get(1)));
                    break;
                case "lineToConstantHeading":
                    trajSeq.lineToXConstantHeading(array[0]);
                    trajSeq.lineToYConstantHeading(array[1]);
                    break;
                case "lineToSplineHeading":
                    trajSeq.lineToXSplineHeading(array[0], Math.toRadians((int)list.get(1)));
                    trajSeq.lineToYSplineHeading(array[1], Math.toRadians((int)list.get(1)));
                    break;
                default:
                    break;
            }
        }

        return trajSeq;
    }
}
