// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

/* Team 3288 Robot: TBD
 * Programmed by Brandon, Colby, and Mr. N
 */

package frc.robot;
import com.revrobotics.CANSparkMax;
import com.revrobotics.RelativeEncoder;
import com.revrobotics.CANSparkMax.IdleMode;
import com.revrobotics.CANSparkMaxLowLevel.MotorType;

import edu.wpi.first.math.controller.PIDController;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.wpilibj.DoubleSolenoid;
import edu.wpi.first.wpilibj.GenericHID;
import edu.wpi.first.wpilibj.PneumaticsModuleType;
import edu.wpi.first.wpilibj.TimedRobot;
import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj.ADIS16470_IMU.IMUAxis;
import edu.wpi.first.wpilibj.DoubleSolenoid.Value;
import edu.wpi.first.wpilibj.GenericHID.RumbleType;
import edu.wpi.first.wpilibj.SPI.Port;
import edu.wpi.first.wpilibj.drive.DifferentialDrive;
import edu.wpi.first.wpilibj.ADIS16470_IMU;
import edu.wpi.first.wpilibj.ADXRS450_Gyro;
import edu.wpi.first.wpilibj.motorcontrol.MotorControllerGroup;
import edu.wpi.first.wpilibj.smartdashboard.SendableChooser;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;

/**
 * The VM is configured to automatically run this class, and to call the functions corresponding to
 * each mode, as described in the TimedRobot documentation. If you change the name of this class or
 * the package after creating this project, you must also update the build.gradle file in the
 * project.
 * 
 * Base code from WPILib don't change
 **/
public class Robot extends TimedRobot {
  // Sets up autonomous routines.
  private static final String kDefaultAuto = "Default - score_drive_balance";
  private static final String kScoreDriveBackAuto = "score_driveBack";
  private static final String kScoreDriveBackScoreAuto = "score_driveBack_score";
  private String m_autoSelected;
  private final SendableChooser<String> m_chooser = new SendableChooser<>();

  /** Definitions **/
  /* Digital objects connected to physical objects. */
  // The controllers.
  private final GenericHID redController = new GenericHID(1); //Operator Controller
  private final GenericHID blueController = new GenericHID(0); //Drive Controller

  
  // Drive motors
  private final CANSparkMax leftMotor1 = new CANSparkMax(1, MotorType.kBrushless);
  private final CANSparkMax leftMotor2 = new CANSparkMax(2, MotorType.kBrushless);
  private final MotorControllerGroup leftDriveMotors = new MotorControllerGroup(leftMotor1, leftMotor2);

  private final CANSparkMax rightMotor1 = new CANSparkMax(3, MotorType.kBrushless);
  private final CANSparkMax rightMotor2 = new CANSparkMax(4, MotorType.kBrushless);
  private final MotorControllerGroup rightDriveMotors = new MotorControllerGroup(rightMotor1, rightMotor2);

  //Drive Motor Encoders
  private final RelativeEncoder leftMotor1Encoder = leftMotor1.getEncoder();
  private final RelativeEncoder leftMotor2Encoder = leftMotor2.getEncoder();
  private final RelativeEncoder rightMotor1Encoder = rightMotor1.getEncoder();
  private final RelativeEncoder rightMotor2Encoder = rightMotor2.getEncoder();
  
  //The Drive Train
  private final DifferentialDrive driveTrain = new DifferentialDrive(leftDriveMotors, rightDriveMotors);

  
  //DoubleSolenoids
  private final DoubleSolenoid firstStage = new DoubleSolenoid(PneumaticsModuleType.REVPH, 1, 2); //Small solenoid
  private final DoubleSolenoid secondStage = new DoubleSolenoid(PneumaticsModuleType.REVPH, 3, 4); //Big solenoid
  private final DoubleSolenoid clamp = new DoubleSolenoid(PneumaticsModuleType.REVPH, 5, 6); //Clamping solenoid

  //Motor for extension of the arm.
  private final CANSparkMax armExtensionMotor = new CANSparkMax(5, MotorType.kBrushed);
  //Extension Motor Encoder
  private final RelativeEncoder armExtensionEncoder = armExtensionMotor.getEncoder();

  //Intake motors
  private final CANSparkMax tooth1 = new CANSparkMax(6, MotorType.kBrushed);
  private final CANSparkMax tooth2 = new CANSparkMax(7, MotorType.kBrushed);
  private final MotorControllerGroup teeth = new MotorControllerGroup(tooth1, tooth2);

  
  //This is the gyro. Sets up the axises. the x axis is the yaw axis, y is the roll, and z is the pitch.
  private final ADIS16470_IMU yaw = new ADIS16470_IMU(IMUAxis.kX, Port.kMXP, null);
  private final ADIS16470_IMU pitch = new ADIS16470_IMU(IMUAxis.kZ, Port.kMXP, null);
  private final ADIS16470_IMU roll = new ADIS16470_IMU(IMUAxis.kY, Port.kMXP, null);

  
  /* PID Controllers */
  //This is the PID control. //Hard code parameters 
  private final PIDController forwardController = new PIDController(0, 0, 0, 0.1);
  private final PIDController angleController = new PIDController(0, 0, 0, 0.1);
  private final PIDController armExtensionController = new PIDController(0, 0, 0, 0.1);
  

  /* Variables for autonomous */
  //set the distance to travel from out of community to balance
  public final double distanceToBalance = 15; //subject to change.

  //Angle needed to turn and grab the cone after driving back. 
  public final double angleToGrabCone = 0; //subject to change.
  
  //Angle needed to turn and drive back to drive station after grabbing cone.
  public final double angleToDriveBack = 0; //subject to change.

  //Distance to cone.
  public final double coneDistance = 10; //subject to change.


  /* Other necessary variables */
  //provides the status of the intake. False is open, and true is closed. Starts in closed position to hold game piece
  public boolean intakeStatus = true;

  //Distance of middle scoring level. needs to be figured out.
  public final double middleNodeDistance = 0;

  //Distance of the high node. Needs to be figured
  public final double highNodeDistance = 1;

  //sets the distance to travel to get out of community in autonomous
  public final double distanceOutOfCommunity = 30; //subject to change.

  //set the override 
  public boolean isOverride = false;


  /*
   * This function is run when the robot is first started up and should be used
   * for any initialization code.
   */
  @Override
  public void robotInit() {
    //Sets up the options you see for auto on SmartDashboard.
    m_chooser.setDefaultOption("Default - score_drive_balance", kDefaultAuto);
    m_chooser.addOption("score_driveBack", kScoreDriveBackAuto);
    m_chooser.addOption("score_driveBack_score", kScoreDriveBackScoreAuto);
    SmartDashboard.putData("Auto choices", m_chooser);

    //This is where we change the setInverted properties on the motors.
    leftMotor1.setInverted(true); //True or false depending on when we test the motors.
    leftMotor2.setInverted(true);
    rightMotor1.setInverted(true);
    rightMotor2.setInverted(true);

    armExtensionMotor.setInverted(true);
    tooth1.setInverted(false);
    tooth2.setInverted(true);

    //Makes the intake closed at the start of the match
    intakeStatus = true;

    //Make sure that it is in starting configuration.
    armLevel(0);

    //Sets up the PID controllers.
    angleController.setTolerance(5);
    angleController.enableContinuousInput(-180, 180);
    armExtensionController.setTolerance(5); 
    forwardController.setTolerance(5); 

    //set the encoders to 0
    //This sets up the encoder of the arm extension
    armExtensionEncoder.setPosition(0);
    leftMotor1Encoder.setPosition(0);
    leftMotor2Encoder.setPosition(0);
    rightMotor1Encoder.setPosition(0);
    rightMotor2Encoder.setPosition(0);
  
  }

  /*
   * This function is called every 20 ms, no matter the mode. Use this for items like diagnostics
   * that you want ran during disabled, autonomous, teleoperated and test.
   *
   * This runs after the mode specific periodic functions, but before LiveWindow and
   * SmartDashboard integrated updating.
   */
  @Override
  public void robotPeriodic() {
    SmartDashboard.putNumber("Time (seconds)", Timer.getFPGATimestamp());
  }

  /*
   * This autonomous (along with the chooser code above) shows how to select between different
   * autonomous modes using the dashboard. The sendable chooser code works with the Java
   * SmartDashboard. If you prefer the LabVIEW Dashboard, remove all of the chooser code and
   * uncomment the getString line to get the auto name from the text box below the Gyro
   *
   * You can add additional auto modes by adding additional comparisons to the switch structure
   * below with additional strings. If using the SendableChooser make sure to add them to the
   * chooser code above as well.
   */
  @Override
  public void autonomousInit() {
    m_autoSelected = m_chooser.getSelected();
    // m_autoSelected = SmartDashboard.getString("Auto Selector", kDefaultAuto);
    System.out.println("Auto selected: " + m_autoSelected);

    //sets all the positions of the encoders to 0
    armExtensionEncoder.setPosition(0);
    leftMotor1Encoder.setPosition(0);
    rightMotor1Encoder.setPosition(0);

    //calibrates the gyro
    yaw.reset();
    pitch.reset();
    
  }


  /*** AUTONOMOUS AND TELEOP METHODS ***/ 
  
  /** CONTROL METHODS USED IN AUTONOMOUS AND TELEOP **/

  /* Method for setting the level of the arm
   * @param int level; 0: starting configuration, 1: ground level, 2: scoring level.
   */
  public void armLevel(int level) {
    switch(level){//Up and down motion of the arm. Need to figure out how to start with this.
      case 0: //Starting configuration  
        armExtension(0, false);//retacks the arm
        
        if(armExtensionEncoder.getPosition() > 5){//Need to do math here. Put arm to starting configuration.
          firstStage.set(Value.kForward);
          secondStage.set(Value.kForward);
        }
        
        break;
      case 1: //Ground Level Configuration
        armExtension(0, false);

        if(armExtensionEncoder.getPosition() > 5){//Need to do math here. Put arm to Ground level
          firstStage.set(Value.kReverse);
          secondStage.set(Value.kReverse);
        }
        
      case 2: //Scoring Level Configuration
        firstStage.set(Value.kForward);
        firstStage.set(Value.kReverse);
        break;
      default:
        break;
    }
  }

  /* This is the method that controls the clamp 
   * @param isClosed is the boolean that controls the intake.
   */
  public void bite(Boolean isClosed) {
    if (isClosed) { //Closed with game peice
      clamp.set(Value.kForward);
      teeth.set(0.75);
    } else { //Open
      clamp.set(Value.kReverse);
      teeth.set(0);
    }       
  }

  /*This is the method that controls the armExtensionMotor during autonmous and resticks it in teleop
   * @param position is the encoder at the position wanted
   * @param override is a boolean that causes a override incase of failier
   */
  public void armExtension(double position, boolean override) {
    
    if (override) {
      armExtensionMotor.set(redController.getRawAxis(1));
    }
    else {
      armExtensionMotor.set(armExtensionController.calculate(armExtensionEncoder.getPosition(), position));
    }
  }


  /** AUTONOMOUS ONLY METHODS **/
  
  /* This method will be used to score on the middle or high nodes during autonomous.
   * @param boolean isHigh is used to say if it is going high or not.
   */
  public void score(boolean isHigh){

    //used to score the game peices on certain levels.
    armLevel(2); //arm to scoring level
    
    //Used to get the time at the time the method is called
    double startofMethod = Timer.getFPGATimestamp();

    if(isHigh){
      //need to check the time. While the arm is shorter than the high node distance and time is more than 3 seconds.
      while((armExtensionEncoder.getPosition() < highNodeDistance) && (Timer.getFPGATimestamp() - startofMethod < 3)) { 
        armExtensionMotor.set(0.75); //Extends arm at speed of 0.75.
      }
    }else{
      while((armExtensionEncoder.getPosition() < middleNodeDistance) && (Timer.getFPGATimestamp() - startofMethod < 3)) { 
        armExtensionMotor.set(0.75); //Extends arm at speed of 0.75.
      }
    }
    
    bite(false);// opens the intake

    //retracts the arm until encoder postion is less than or time is 6sec.
    while((armExtensionEncoder.getPosition() < 1) && (Timer.getFPGATimestamp() - startofMethod < 6 )) { 
      armExtensionMotor.set(-0.75);
    }
  }

  /* Method for returning the encoder that has leased number of rotions.
   * @return returns a relative encoder
   */
  public RelativeEncoder distanceDriven(){
    if (leftMotor1Encoder.getPosition() > rightMotor1Encoder.getPosition()){
      return rightMotor1Encoder;
    }
      return leftMotor1Encoder;

  }

  /* Method for driving a certain distance. Uses the .getPosition() from an encoder to return the number of rotations.
   * @param distance for the distance for the robot to drive.
   */
  public void driveDistance(double distance){

    //Used to get the time at the time the method is called
    double startofMethod = Timer.getFPGATimestamp();

    //drives the distance passed in by the paramiter out of the community.
    while((distanceDriven().getPosition() < -distance) && (Timer.getFPGATimestamp() - startofMethod  < 5)) { 
      driveTrain.arcadeDrive(forwardController.calculate(distanceDriven().getPosition(), distance), 0);
    }
  }

  /* Method that is an autonomous rotine. This would be selected in autonomous periotic
   * Would score high and then drive backwards out of the community.
   */
  public void scoreDriveBack() {

    //this method scores on the different levels. Right now it is used to score high. Ends retracted
    score(true);
    
    //drives backward out of the community
    driveDistance(distanceOutOfCommunity);
  }

  /* Method that is an autonomous routine. This would be selected in autonomous periotic
   * Would score high and then drive backwards out of the community. After that it drives forward to balance
   * on the charging station. 
   */
  public void scoreDriveBackBalance() {
    scoreDriveBack();//runs scoreDrvieBack

    //drives back to the charging station.
    driveDistance(distanceToBalance);
    
    //Gets the gyro numbers
    

    //Using the gyro to ballance
  }

  /* Method that is an autonomous routine. This would be selected in autonomous periotic.
   * Will score the cone on the high node and drive backwards out of the community and 
   * then turn to get another cone then turn again drive to the grid and score again.
   * By far this is the most complicated one.
   */
  public void scoreDriveScore() {
    scoreDriveBack();

    //turns the robot to the cone 
    while((yaw.getAngle() < 90) && (Timer.getFPGATimestamp() < 5)) {
      driveTrain.arcadeDrive(0, angleController.calculate(yaw.getAngle(), angleToGrabCone));
    }
    
    //drives into the cone
    driveDistance(coneDistance);
    
    //grabs it
    bite(true);

    //turns back to driver's station
    while((yaw.getAngle() < 135) && (Timer.getFPGATimestamp() < 5)){
      driveTrain.arcadeDrive(0, angleController.calculate(yaw.getAngle(), angleToDriveBack));
    }

    //drives back to driver station and scores
    driveDistance(distanceOutOfCommunity);
    score(true);
  }


  /* This function is called periodically during autonomous. */
  @Override
  public void autonomousPeriodic() {
    switch (m_autoSelected) {
      case kScoreDriveBackAuto:
        scoreDriveBack();
        break;
      case kScoreDriveBackScoreAuto:
        scoreDriveScore();
        break;
      case kDefaultAuto:
      default:
        scoreDriveBackBalance();
        break;
    }
  }

  /* This function is called once when teleop is enabled. */
  @Override
  public void teleopInit() {
    //Sets motors IdleMode to coast. Need to add 3 second wait. Figure it out, take your time. Maybe use boolean for brakes.
    leftMotor1.setIdleMode(IdleMode.kCoast);
    leftMotor2.setIdleMode(IdleMode.kCoast);
    rightMotor1.setIdleMode(IdleMode.kCoast);
    rightMotor2.setIdleMode(IdleMode.kCoast);
  }

  /* This function is called periodically during operator control. */
  @Override
  public void teleopPeriodic() {
    
    //Sets up the drive train. Left stick controls the forward and back. Right controls turning.
    //Want cubic function. currently linear. Look up deadbands
    driveTrain.arcadeDrive(blueController.getRawAxis(1), blueController.getRawAxis(4));

    //need control for exstention motor

    //This bunch of if then statements is the button map. Blue controller is operator
    if (redController.getRawButton(0)) { // Button ✖️. Scoring position
      armLevel(2);
    } else if (redController.getRawButton(1)) { // Button ⭕. Intake level
      armLevel(1);
    } else if (redController.getRawButton(2)) { // Button 🟪. Starting configeration
      armLevel(0);
    } else if (redController.getRawButton(3)) { // Button 🔺. No purpose at the moment.
      
    } else if (redController.getRawButton(4)) { // Button L1. Intake is open
      intakeStatus = false;
    } else if (redController.getRawButton(5)) { // Button R1. Intake is closed
      intakeStatus = true;
    } else if (redController.getRawButton(6)) { // Button SHARE.
      if (isOverride) {
        isOverride = false;
      } else {
        isOverride = true;
      }
    } else if (redController.getRawButton(7)) { // Button OPTIONS.

    } else if (redController.getRawButton(8)) { // Button L3.
      
    } else if (redController.getRawButton(9)) { // Button R3.

    }

    armExtension(redController.getRawAxis(1), isOverride);//controls the extension of the arm

    bite(intakeStatus);//controls the intake. false is open, true is closed

    //sends values to our smartdashbourd.
    SmartDashboard.putNumber("Yaw angle", yaw.getAngle());
    SmartDashboard.putNumber("", kDefaultPeriod);
  }

  /* This function is called once when the robot is disabled. */
  @Override
  public void disabledInit() {}

  /* This function is called periodically when disabled. */
  @Override
  public void disabledPeriodic() {}

  /* This function is called once when test mode is enabled. */
  @Override
  public void testInit() {}

  /* This function is called periodically during test mode. */
  @Override
  public void testPeriodic() {}

  /* This function is called once when the robot is first started up. */
  @Override
  public void simulationInit() {}

  /* This function is called periodically whilst in simulation. */
  @Override
  public void simulationPeriodic() {}
}

























