duration=10;

%Save serial data
data = readserialio('COM1', duration);
dlmwrite('testdata/frontal_lobe_10s_teeth_1.txt', data);