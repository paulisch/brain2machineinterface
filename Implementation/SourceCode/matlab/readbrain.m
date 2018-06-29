duration=10;
%Load serial data
data = dlmread('testdata/frontal_lobe_10s_mouth_open_close.txt');
samplingrate=256;
[rows, cols] = size(data);
k=0:cols-1;

sample = data(1, 1:end);
%sample = smooth(sample);
%sample = detrend(sample, 0);

%figure;
%plot(sample);

filteredData = bpfilt(sample, 1, 49, samplingrate, 0);
%sample = filteredData;
%sample = detrend(sample, 0);

figure;
plot(k./samplingrate, sample);

%Fourier
y=fft(sample(8*256+1:9*256));
yp=abs(y); %Amptlitudengang
yang=angle(y); %Phasengang
figure;
%stem(k./duration, yp./(duration*samplingrate/2));
%stem(k./duration, yp);
stem(yp);

sum(yp(20:128))/(128-20+1);