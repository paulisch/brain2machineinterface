duration=10;
%Load serial data
data = dlmread('testdata/frontal_lobe_10s_look_left_3_times.txt');
samplingrate=256;
[rows, cols] = size(data);
k=0:cols-1;

sample = data(2, 1:end);
%sample = smooth(sample);
%sample = detrend(sample, 0);

%sample1 = data(1, 1:end);
%sample2 = data(2, 1:end);

%figure;
%hold on;
%plot(k./samplingrate, sample1);
%plot(k./samplingrate, sample2);
%legend('CH1', 'CH2');
%hold off;

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