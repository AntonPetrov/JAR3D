% pMakeModelFromSearchSaveFile(Search) creates an SCFG/MRF Node variable corresponding to the model in Search

% pMakeModelFromSearchSaveFile('LIB00002 IL 2008-03-20_23_29_25-Sarcin_13_flanked_by_cWW_in_1s72')
% Search = 'LIB00002 IL 2008-03-20_23_29_25-Sarcin_13_flanked_by_cWW_in_1s72';

% load LIB00014_IL_tSH-tSH-tHS-tHS.mat
% pMakeModelFromSearchSaveFile(Search,'IL',1);

% load MotifLibrary\IL\0.6\IL_23262.1.mat

function [Node,Search] = pMakeMotifModelFromSSF(Search,Param,Prior,loopType,UseIndex)

Normalize = 1;
if length(Param) > 7
    Normalize = Param(8);
end

if nargin < 2,
    Param   = 0;
    Verbose = 0;
else
    Verbose = Param(1);
end

if nargin <3,
    Prior = [10000 10000 10000 10000 0];    % Extremely strong prior, makes for flat letter distribution
end

if nargin <4,
    loopType = 'IL';              % Assume internal loops if type not specified
end

if nargin <5,
    [seqNum,] = size(Search.Candidates);    % Use all instances
    UseIndex = 1:seqNum;
end

while length(Prior) < 5,
    Prior = [Prior 0];
end

% ----------------------------------- Load Search from filename, if applicable

if strcmp(class(Search),'char'),
  load(['MotifLibrary' filesep Search],'Search','-mat');
end

% ----------------------------------- Gather basic information about the search

[L,N] = size(Search.Candidates);        % L = num instances; N = num NT
N = N - 1;                              % number of nucleotides

f = Search.Candidates(:,N+1);           % file numbers of motifs

File = Search.File(f(1));                      % file of query motif
NTNumber = double(Search.Candidates(1,1));     % index of first NT
LastNTNumber = double(Search.Candidates(1,N)); % index of last NT

% ----- Display interactions in the first instance

if Verbose > 0,
  i = Search.Candidates(1,1:N);            % indices of query motif, sometimes
  fprintf('Interactions in the first instance:\n');
  zShowInteractionTable(File,full(i));
end

% ----- Identify where the flanking pair is, to split up the two strands
% ----- This should be replaced with Anton's variable in Search that
% ----- identifies them
% --------------------------------------- Find locations of truncations

[Edge,BPh,BR,Search] = pConsensusInteractions(Search,Verbose); 
                                        % find consensus interactions

F.Edge = Edge;                          % consensus pairs and stacks
F.BasePhosphate = BPh;
F.BaseRibose = BR;

Search.Edge = Edge;                     % save these to pass them back
Search.BPh  = BPh;
Search.BR   = BR;

if sum(sum(BPh .* (1-eye(size(BPh))))) > 0 && Verbose > 0,
  [i,j,k] = find(BPh);
  for a = 1:length(i),
    if i(a) ~= j(a) && Verbose > 0,
      fprintf('pMakeMotifModelFromSSF: Conserved base-phosphate: %d %d %s\n', i(a), j(a), zBasePhosphateText(k(a)));
    end
  end
end

if sum(sum(BR .* (1-eye(size(BR))))) > 0 && Verbose > 0,
  [i,j,k] = find(BR);
  for a = 1:length(i),
    if i(a) ~= j(a) && Verbose > 0,
      fprintf('pMakeMotifModelFromSSF: Conserved base-ribose: %d %d %s\n', i(a), j(a), zBaseRiboseText(k(a)));
    end
  end
end

Search.Truncate = zFindTruncationLocation(loopType,F.Edge);
Truncate = Search.Truncate;

if Verbose > 0,
  fprintf('pMakeMotifModelFromSSF: Truncate = %4d\n', Truncate);
end

% ---------------------------------------- Extract a motif signature

if strcmp(loopType,'HL')
    strands = 1;
else
    strands = 2;
end

    if strcmp(loopType,'IL'),
      [Signature,AllSig,AllPhonetic] = zMotifSignature(F.Edge,strands,1,1,Param);
    else
      [Signature,AllSig,AllPhonetic] = zMotifSignature(F.Edge,strands,1,2,Param);
    end
    if Verbose > 0,
      fprintf('pMakeMotifModelFromSSF:  Signature: %s, Phoneme: %s\n', Signature, AllPhonetic{1});
    end

    Phonetic = AllPhonetic{1};

    if strcmp(loopType,'IL'),
        RSignature = AllSig{2};
        RPhonetic = AllPhonetic{2};
        if Verbose > 0,
          fprintf('pMakeMotifModelFromSSF:  RSignature: %s\n', RSignature);
        end
    else
        RSignature = Signature;
        RPhonetic = Phonetic;
    end

if 0 > 1,
    Signature = 'trouble';
    RSignature = 'trouble';
    Phonetic = 'trouble';
    RPhonetic = 'trouble';

    fprintf('pMakeMotifModelFromSSF: problem making the signature\n');
    full(min(30,abs(F.Edge)))
end

% ---------------------------------------- Save parameters to pass back

Search.Signature  = Signature;
Search.RSignature = RSignature;           %
Search.Phonetic   = Phonetic;
Search.RPhonetic  = RPhonetic;           %

% -------------------------------- Make the model for the consensus structure

F.NT = File.NT(Search.Candidates(1,1:N));   % use the first candidate as model
F.Crossing = zeros(N,N);                    % small enough, pretend none
F.Range    = zeros(N,N);
F.NumNT    = length(F.NT);

if length(Truncate) > 0,                    % at least two strands
    b = 1:N;
    for t = 1:N,
        b(t) = b(t) + 100*sum(t >= Truncate);
    end
    binv = 1:max(b);                                  % invert the spreading
    binv(b) = 1:N;
else
    b = 1:N;
    binv = 1:N;
end

FF.Filename      = File.Filename;
FF.Edge(b,b)     = F.Edge;                        % spread the strands out
FF.NT(b)         = F.NT;
FF.Crossing(b,b) = F.Crossing;
FF.Range(b,b)    = F.Range;
FF.BasePhosphate(b,b) = F.BasePhosphate;
FF.BaseRibose(b,b) = F.BaseRibose;

if Verbose > 0,
  disp('pMakeMotifModelFromSSF:  Consensus interaction table with nucleotides from the first candidate:');
  zShowInteractionTable(FF,b);
end

Node = pMakeNodes(FF,Param,1,b(N),Truncate);          % make the SCFG/MRF model

for n = 1:length(Node),
  Node(n).LeftIndex    = binv(Node(n).LeftIndex);
  Node(n).RightIndex   = binv(Node(n).RightIndex);
  Node(n).MiddleIndex  = binv(Node(n).MiddleIndex);
  Node(n).InterIndices = binv(Node(n).InterIndices);
end

if Verbose > 0,
  for n = 1:length(Node),
    fprintf('%d %s\n', n, Node(n).type);
  end
end

% -------------------------------------- Insert initial node after cluster

m = 1;
for n = 1:length(Node),
  NNode(m) = Node(n);
  NNode(m).nextnode = m+1;
  if strcmp(Node(n).type,'Cluster'),
    if ~strcmp(Node(n+1).type,'Initial'),
      if Verbose > 0,
        fprintf('pMakeMotifModelFromSSF:  Adding an Initial node after Cluster\n');
      end
      m = m + 1;
      NNode(m).type       = 'Initial';
      NNode(m).nextnode   = m+1;
      NNode(m).LeftIndex  = max(NNode(m-1).LeftIndex)+1;
      NNode(m).RightIndex = min(NNode(m-1).RightIndex)-1;
      NNode(m).leftLengthDist = [0.9999 0.0001];
      NNode(m).rightLengthDist = [0.9999 0.0001];
      if Normalize == 1, 
        NNode(m).leftLetterDist = [1 1 1 1]/4;
        NNode(m).rightLetterDist = [1 1 1 1]/4;
      else
        NNode(m).leftLetterDist = [1 1 1 1];
        NNode(m).rightLetterDist = [1 1 1 1];
      end
      NNode(m).Comment    = ' // New Initial node after Cluster';
    end
  end
  m = m + 1;
end

Node = NNode;

if Verbose > 0,
  for n = 1:length(Node),
    fprintf('%d %s\n', n, Node(n).type);
  end
end

% ---------------------------- Turn conserved insertion(s) after Basepair into Initial Node

m = 1;
for n = 1:length(Node),
  NNode(m) = Node(n);                 % store this node
  NNode(m).nextnode = m+1;
  if strcmp(Node(n).type,'Basepair'),
    if ~strcmp(Node(n+1).type,'Initial'),
      J = min(Node(n+1).LeftIndex) - Node(n).LeftIndex;
      K = Node(n).RightIndex - max(Node(n+1).RightIndex);
      if J > 1 || K > 1,              % conserved NT here
        if Verbose > 0,
          fprintf('pMakeMotifModelFromSSF:  Adding an Initial node after a Basepair\n');
        end
        m = m + 1;
        NNode(m).type       = 'Initial';
        NNode(m).nextnode   = m+1;
        NNode(m).LeftIndex  = max(NNode(m-1).LeftIndex)+1;
        NNode(m).RightIndex = min(NNode(m-1).RightIndex)-1;
        NNode(m).leftLengthDist = [0.9999 0.0001];
        NNode(m).rightLengthDist = [0.9999 0.0001];
        if Normalize == 1,
            NNode(m).leftLetterDist = [1 1 1 1]/4;
            NNode(m).rightLetterDist = [1 1 1 1]/4;
        else
            NNode(m).leftLetterDist = [1 1 1 1];
            NNode(m).rightLetterDist = [1 1 1 1];
        end
        NNode(m).Comment         = ' // New Initial node after Basepair node';
      end
    end
  end
  m = m + 1;
end

Node = NNode;

if Verbose > 0,
  for n = 1:length(Node),
    fprintf('%d %s\n', n, Node(n).type);
  end
end

% ------------------------------ Turn initial nodes with conserved bases into alternating Fixed and Initial with nothing

m = 1;
for n = 1:length(Node),
  NNode(m) = Node(n);                 % store this node, keep it
  NNode(m).nextnode = m+1;
  if strcmp(Node(n).type,'Initial'),
    J = min(Node(n+1).LeftIndex) - Node(n).LeftIndex;
    if J > 0,                         % conserved NT here
      for j = 1:J,                    % loop through fixed positions
        if Verbose > 0,
          fprintf('pMakeMotifModelFromSSF:  Replacing Initial with Fixed on the left\n');
        end
        m = m + 1;
        NNode(m).type       = 'Fixed';
        NNode(m).nextnode   = m+1;
        NNode(m).LeftIndex  = max(NNode(m-1).LeftIndex);
        NNode(m).RightIndex = min(NNode(m-1).RightIndex);
        NNode(m).LeftLetter = '';
        NNode(m).RightLetter= '';
        NNode(m).Delete     = 0.001;
        NNode(m).leftLengthDist = [0 1];
        NNode(m).rightLengthDist = [1];
        if Normalize == 1,
            NNode(m).leftLetterDist = [1 1 1 1]/4;
            NNode(m).rightLetterDist = [1 1 1 1]/4;
        else
            NNode(m).leftLetterDist = [1 1 1 1];
            NNode(m).rightLetterDist = [1 1 1 1];
        end
        NNode(m).Comment    = [' // Fixed node on left'];
        if Verbose > 0,
          fprintf('pMakeMotifModelFromSSF:  Adding an Initial node after Fixed\n');
        end
        m = m + 1;
        NNode(m).type       = 'Initial';
        NNode(m).nextnode   = m+1;
        NNode(m).LeftIndex  = max(NNode(m-1).LeftIndex)+1;
        NNode(m).RightIndex = min(NNode(m-1).RightIndex);
        NNode(m).leftLengthDist = [0.9999 0.0001];
        NNode(m).rightLengthDist = [0.9999 0.0001];
        if Normalize == 1,
            NNode(m).leftLetterDist = [1 1 1 1]/4;
            NNode(m).rightLetterDist = [1 1 1 1]/4;
        else
            NNode(m).leftLetterDist = [1 1 1 1];
            NNode(m).rightLetterDist = [1 1 1 1];
        end
        NNode(m).Comment         = ' // New Initial node after Fixed node';
      end
    end
    K = Node(n).RightIndex - max(Node(n+1).RightIndex);
    if K > 0,
      for j = 1:K,                 % loop through fixed positions
        if Verbose > 0,
          fprintf('pMakeMotifModelFromSSF:  Replacing Initial with Fixed on the right\n');
        end
        m = m + 1;
        NNode(m).type       = 'Fixed';
        NNode(m).nextnode   = m+1;
        NNode(m).LeftIndex  = max(NNode(m-1).LeftIndex);
        NNode(m).RightIndex = min(NNode(m-1).RightIndex);
        NNode(m).LeftLetter = '';
        NNode(m).RightLetter= '';
        NNode(m).Delete     = 0.001;
        NNode(m).leftLengthDist  = [1];
        NNode(m).rightLengthDist = [0 1];
        if Normalize == 1,
            NNode(m).leftLetterDist  = [1 1 1 1]/4;
            NNode(m).rightLetterDist = [1 1 1 1]/4;
        else
            NNode(m).leftLetterDist  = [1 1 1 1];
            NNode(m).rightLetterDist = [1 1 1 1];
        end
        NNode(m).Comment    = [' // Fixed node on right'];
        if Verbose > 0,
          fprintf('pMakeMotifModelFromSSF:  Adding an Initial node after Fixed\n');
        end
        m = m + 1;
        NNode(m).type       = 'Initial';
        NNode(m).nextnode   = m+1;
        NNode(m).LeftIndex  = max(NNode(m-1).LeftIndex);
        NNode(m).RightIndex = min(NNode(m-1).RightIndex)-1;
        NNode(m).leftLengthDist  = [0.9999 0.0001];
        NNode(m).rightLengthDist = [0.9999 0.0001];
        if Normalize == 1,
            NNode(m).leftLetterDist  = [1 1 1 1]/4;
            NNode(m).rightLetterDist = [1 1 1 1]/4;
        else
            NNode(m).leftLetterDist  = [1 1 1 1];
            NNode(m).rightLetterDist = [1 1 1 1];    
        end
        NNode(m).Comment         = ' // New Initial node after Fixed node';
      end
    end
  end
  m = m + 1;
end

Node = NNode;

if Verbose > 0,
  for n = 1:length(Node),
    fprintf('%d %s\n', n, Node(n).type);
  end
end

if 0 > 1,
  Text = pNodeToSCFGModelText(Node,5);
  for r = 1:length(Text),
    fprintf('%s\n', Text{r});
  end
end

% ---------------------------- Remove Initial Node after Basepair

clear NNode

Keep = 1:length(Node);
for n = 1:(length(Node)-1),
  if strcmp(Node(n).type,'Basepair') && strcmp(Node(n+1).type,'Initial'),
    Keep(n+1) = 0;
    if Verbose > 0,
      fprintf('pMakeMotifModelFromSSF:  Removing Initial node after Basepair\n');
    end
  end
end

  Node = Node(find(Keep));
  for n = 1:(length(Node)-1),
    Node(n).nextnode = n + 1;
  end

  if 0 > 1,
    for n = 1:length(Node),
      fprintf('%d %s\n', n, Node(n).type);
    end
  end

% ---------------------------- Show alignment of instances

  Text = xAlignCandidates(Search.File,Search);

  if Verbose > 0,
    disp('pMakeMotifModelFromSSF: alignment of instances from 3D');
    for t = 1:length(Text),
      fprintf('%s\n', Text{t});
    end
  end

  % ------------------------------------ Modify pair and substitution probs

  [Node,Search] = pUpdateModelWithSSF(Node,Search,f,F,Param,Prior,loopType,File,UseIndex,Normalize);

  if Verbose > 0,
    fprintf('\n')
  end

end
