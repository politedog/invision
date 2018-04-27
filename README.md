# invision
Invision Android Prototype Generator
java -jar invision.jar _path_to_invision_ _path_to_project_
    You need to extract the json from Invision's index.html...
    Correct all the keys that aren't in quotes...
    And write that to index_pretty.json in the same directory.
    I find these vim commands helpful:
    :%s/^\\s*/
    :%s/^[a-zA-Z][a-zA-Z]*/\"\\0\
