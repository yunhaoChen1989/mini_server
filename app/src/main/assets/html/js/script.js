
const ul = document.getElementById("files");
let page=0;
let pageSize=10;
loadPic();



document.addEventListener('scroll', function () {
    const scrollTop = window.scrollY || document.documentElement.scrollTop;
    const scrollHeight = document.documentElement.scrollHeight;
    const clientHeight = document.documentElement.clientHeight;

    if (scrollTop + clientHeight >= scrollHeight - 5) {
        // User has scrolled to the bottom
        page++
        loadPic()
    }
});

document.getElementById('fileName').addEventListener('change', function () {
    const fileInput = document.getElementById('fileName');

    if (fileInput.files.length > 0) {
        const file = fileInput.files[0];
        const formData = new FormData();
        formData.append('file', file);

        // Perform the file upload using Fetch API
        fetch('/upload', {
            method: 'POST',
            body: formData
        })
            .then(response => response.json())
            .then(data => {
                console.log('Success:', data);
                alert(decodeURIComponent(data.data));
            })
            .catch(error => {
                console.error('Error:', error);
                alert('File upload failed');
            });
    } else {
        alert('Please choose a file first');
    }
});
function showPic(ele){
    for(let i=0; i<ele.length; i++){
        const li= document.createElement("li");
        const fileName = decodeURIComponent(ele[i])
        li.innerHTML="<img src='file?fileName="+ fileName +"'/><span><a href='/download?fileName="+ fileName +"'>Download</a></span><span>"+ fileName +"</span>"
        ul.appendChild(li)
    }
}

function loadPic(){
    const url = `/listFiles?page=${page}&pageSize=${pageSize}`;
    fetch(url)
        .then((response)=>{
            return response.json();
        })
        .then((json)=>{
            if(json && json.length > 0){
                console.log(json[0]);
                showPic(json)
            }

        })
}

