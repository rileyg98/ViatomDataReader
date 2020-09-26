# ViatomDataReader
Reads raw binary files extracted from Viatom pulse oximeters. 

Inspiration (and the header definition) was borrowed from OSCAR, over at https://gitlab.com/pholy/OSCAR-code.

This takes an input file name and an output file name. It does basic sanity checking of the file structure.

This will output a CSV formatted with the following columns: Unix Timestamp, Pulse, Heartrate.

I've only tested this on a CheckMeO2 and the ViHealth output file (not mentioned over at OSCAR's wiki, but you will find that on the Android app that it saves a dated file with the raw data).

I found monitor mode did not work for collection and processing by this code.
