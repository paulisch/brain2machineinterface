duration=10;

%Save serial data
data = readserial('COM1', duration);
dlmwrite('testdata/frontal_lobe_10s_no_gesture2.txt', data);