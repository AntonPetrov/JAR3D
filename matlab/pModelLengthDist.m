function [L] = pModelLengthDist(Node)
    L(1,1) = 1;
    N = length(Node);
    for n = 1:N
        switch Node(n).type
            case 'Initial'
                A = zeros(length(Node(n).leftLengthDist),1);
                A(:,1) = Node(n).leftLengthDist;
                L = conv2(L,A);
                B = zeros(1,length(Node(n).rightLengthDist));
                B(1,:) = Node(n).rightLengthDist;
                L = conv2(L,B);
            case 'Fixed'
                A = zeros(length(Node(n).leftLengthDist),1);
                A(:,1) = Node(n).leftLengthDist;
                L = conv2(L,A);
                B = zeros(1,length(Node(n).rightLengthDist));
                B(1,:) = Node(n).rightLengthDist;
                L = conv2(L,B);
            case 'Basepair'
                I = zeros(2);
                D = Node(n).Delete;
                I(1,1) = D;
                I(2,2) = 1-D;
                L = conv2(L,I);
                A = zeros(length(Node(n).leftLengthDist),1);
                A(:,1) = Node(n).leftLengthDist;
                A = A*(1-D);
                A(1,1) = A(1,1)+D;
                L = conv2(L,A);
                B = zeros(1,length(Node(n).rightLengthDist));
                B(1,:) = Node(n).rightLengthDist;
                B = B*(1-D);
                B(1,1) = B(1,1)+D;
                L = conv2(L,B);
            case 'Cluster'
                I = zeros(2);
                D = Node(n).Delete;
                I(1,1) = D;
                I(length(Node(n).Left)+1,length(Node(n).Right)+1) = 1-D;
                L = conv2(L,I);
                for i = 1:length(Node(n).Insertion)
                    if Node(n).Insertion(i).Position <= length(Node(n).Left) %Left side insertion
                        A = zeros(length(Node(n).Insertion(i).LengthDist),1);
                        A(:,1) = Node(n).Insertion(i).LengthDist;
                        A = A*(1-D);
                        A(1,1) = A(1,1)+D;
                        L = conv2(L,A);
                    else %Right side insertion
                        B = zeros(1,length(Node(n).Insertion(i).LengthDist));
                        B(1,:) = Node(n).Insertion(i).LengthDist;
                        B = B*(1-D);
                        B(1,1) = B(1,1)+D;
                        L = conv2(L,B);
                    end
                end
            case 'Hairpin'
                if strcmp(Node(n).LeftLetter,'*')  % IL capping hairpin doesn't contribute to length
                    return
                else                          % Hairpin loop, collapse matrix and add hairpin length
                    [a,b] = size(L);
                    L1 = zeros(a+b,1);
                    for i = 1:a
                        for j = 1:b
                            L1(i+j-1) = L1(i+j-1) + L(i,j);
                        end
                    end
                    c = Node(n).RightIndex-Node(n).LeftIndex + 1;
                    L2 = zeros(c+1,1);
                    L2(c+1) = 1;
                    for i = 1:length(Node(n).Insertion)
                        L3 = zeros(1,length(Node(n).Insertion(i).LengthDist));
                        L3(1,:) = Node(n).Insertion(i).LengthDist;
                        L2 = conv2(L2,L3');
                    end
                    L = conv2(L1,L2);
                end
        end
    end
end
