duration=10;

%Save serial data
data = readserialio('COM1', duration);
dlmwrite('testdata/frontal_lobe_10s_look_left_3_times.txt', data);