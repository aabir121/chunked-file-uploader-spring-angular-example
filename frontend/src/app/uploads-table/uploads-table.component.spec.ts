import { ComponentFixture, TestBed } from '@angular/core/testing';

import { UploadsTableComponent } from './uploads-table.component';

describe('UploadsTableComponent', () => {
  let component: UploadsTableComponent;
  let fixture: ComponentFixture<UploadsTableComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [UploadsTableComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(UploadsTableComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
