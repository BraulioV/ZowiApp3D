package com.jalcdeveloper.zowiapp.io;

public interface ZowiProtocol{
    char SEPARATOR = ' ';
    String FINAL = "\n\r";

    char MOVE_COMMAND = 'M';
    char MOVE_STOP_OPTION = '0';
    char MOVE_WALK_FORWARD_OPTION = '1';
    char MOVE_WALK_BACKWARD_OPTION = '2';
    char MOVE_TURN_LEFT_OPTION = '3';
    char MOVE_TURN_RIGHT_OPTION = '4';
    char MOVE_UPDOWN_OPTION = '5';
    char MOVE_MOONWALKER_LEFT_OPTION = '6';
    char MOVE_MOONWALKER_RIGHT_OPTION = '7';
    char MOVE_SWING_OPTION = '8';
    String MOVE_CRUSAITO_LEFT_OPTION = "9";
    String MOVE_CRUSAITO_RIGHT_OPTION = "10";
    String MOVE_JUMP_OPTION = "11";

    char ACK_COMMAND = 'A';
    char FINAL_ACK_COMMAND = 'F';

    char BATTERY_COMMAND = 'B';
    char PROGRAMID_COMMAND = 'I';
}