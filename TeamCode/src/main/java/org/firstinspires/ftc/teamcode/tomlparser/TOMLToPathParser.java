package org.firstinspires.ftc.teamcode.tomlparser;

import static org.firstinspires.ftc.robotcore.external.BlocksOpModeCompanion.hardwareMap;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Environment;

import com.acmerobotics.roadrunner.Pose2d;
import com.acmerobotics.roadrunner.TrajectoryActionBuilder;
import com.qualcomm.robotcore.util.WebHandlerManager;
//import com.acmerobotics.roadrunner.Vector2d;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.firstinspires.ftc.ftccommon.external.WebHandlerRegistrar;
import org.firstinspires.ftc.robotcore.internal.webserver.WebHandler;
import org.firstinspires.ftc.teamcode.MecanumDrive;
import org.tomlj.Toml;
import org.tomlj.TomlArray;
import org.tomlj.TomlPosition;
import org.tomlj.TomlTable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    ArrayList<Path> tomlSequences = new ArrayList<>();

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
            //instanciating a new Path
            Path path = new Path(file.getName(), Toml.parse(file.getPath()).getTable("sequence"), Toml.parse(file.getPath()).getTable("initialPosition"));
            //Need to connect the initial position of a file to the main sequences (weird quirk in how trajectory sequence and roadrunner works)
            tomlSequences.add(path);
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
        * If its alphabetical:
        * 0, 1 - first alpha
        * 2, 3 - second alpha
        * 2n, and 2n+1 as the two important files, with 2n being sequence and 2n+1 being initialPosition
        * */

        // Gets the initial values of each
        double lastHeading = (double) ((Path.search(filename, tomlSequences).initialPosition.getArray("initialPosition")).toList()).get(2);
        double lastX = (double) ((Path.search(filename, tomlSequences).initialPosition.getArray("initialPosition")).toList()).get(0);
        double lastY = (double) ((Path.search(filename, tomlSequences).initialPosition.getArray("initialPosition")).toList()).get(1);

        //Creates Pose of the starting values, and initializes the drive
        Pose2d startPose = new Pose2d(lastX, lastY, Math.toRadians(lastHeading));
        MecanumDrive drive = new MecanumDrive(hardwareMap, startPose);

        //creates the Trajectory Builder that we use to create the paths, and creates an instance of the main sequence
        TrajectoryActionBuilder trajSeq = drive.actionBuilder(startPose);
        TomlTable mainPath = Path.search(filename, tomlSequences).mainPath;
        for (int i = 0; i < mainPath.getArray("sequences").size() - 1; i++)
        {
            //creates the specific list for this specific trajectory point, and creates an array for x, y coordinates
            List trajectoryList = (List)mainPath.get("sequence.traj" + (i+1) + ".args");
            double[] array = (double[]) trajectoryList.get(0); // x, y

            //handle types of paths (i.e. constant, linear, spline, etc.
            switch  ((String)trajectoryList.get(0)) {
                case "lineToLinearHeading":
                    trajSeq.lineToXLinearHeading(array[0], Math.toRadians((int)trajectoryList.get(1)));
                    trajSeq.lineToYLinearHeading(array[1], Math.toRadians((int)trajectoryList.get(1)));
                    break;
                case "lineToConstantHeading":
                    trajSeq.lineToXConstantHeading(array[0]);
                    trajSeq.lineToYConstantHeading(array[1]);
                    break;
                case "lineToSplineHeading":
                    trajSeq.lineToXSplineHeading(array[0], Math.toRadians((int)trajectoryList.get(1)));
                    trajSeq.lineToYSplineHeading(array[1], Math.toRadians((int)trajectoryList.get(1)));
                    break;
                default:
                    break;
            }
        }

        return trajSeq;
    }
}

class Path
{
    //Class used to link initial position and the main path components because the way
    //the toml formatting works is kinda goofy
    public TomlTable initialPosition;
    public TomlTable mainPath;
    public String name;

    //Constructor
    public Path(String name, TomlTable mainPath, TomlTable initialPosition)
    {
        this.name = name;
        this.mainPath = mainPath;
        this.initialPosition = initialPosition;
    }

    //Searches for the correct file in the array of all paths
    public static Path search(String name, ArrayList<Path> list)
    {
        for (Path p : list)
        {
            if ((p.name).equals(name))
            {
                return p;
            }
        }
        //returns a default if name is not found (probably will just throw an error somewhere)
        return new Path("You screwed up broski", null, null);
    }
}
