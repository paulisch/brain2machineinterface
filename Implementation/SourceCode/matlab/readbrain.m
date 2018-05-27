duration=10;

% %Save serial data
% data = readserial('COM1', duration);
% dlmwrite('testdata/frontal_lobe_10s_look_open_close_mouth.txt', data);

%Load serial data
data = dlmread('testdata/frontal_lobe_10s_no_gesture2.txt');
samplingrate=256;
[rows, cols] = size(data);
k=0:samplingrate*duration-1;

sample = data(1, 1:end);
sample = detrend(sample, 0);

%figure;
%plot(sample);

filteredData = bpfilt(sample, 1, 50, samplingrate, 0);
sample = filteredData;
sample = detrend(sample, 0);

figure;
plot(sample);

%Fourier
y=fft(sample);
yp=abs(y); %Amptlitudengang
yang=angle(y); %Phasengang
figure;
%stem(k./duration, yp./(duration*samplingrate/2));
stem(k./duration, yp);
%stem(yp);