<?php
    $uploaded_file_path = "/Users/csaikia/Dropbox/Coursework/CSE535/uploaded_files/";
    $file_path = $uploaded_file_path . basename( $_FILES['files']['name']);
    if(!move_uploaded_file($_FILES['files']['tmp_name'], $file_path)) {
        echo "fail";
    }
    # TODO comment this line to add the code for comparator
    #$mystring = system('python comparator.py 1', $retval);
    #echo $file_path;
    $mystring = system('python comparator.py '.$file_path, $retval);
    system('rm '.$file_path, $ret);
    echo json_encode(array('exit_code' => $retval));
 ?>
